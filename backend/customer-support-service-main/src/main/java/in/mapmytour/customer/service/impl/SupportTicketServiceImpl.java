package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.entity.SupportTicket;
import in.mapmytour.customer.entity.CustomerSupportAgent;
import in.mapmytour.customer.exception.ResourceNotFoundException;
import in.mapmytour.customer.exception.ServiceException;
import in.mapmytour.customer.repository.CustomerSupportAgentRepository;
import in.mapmytour.customer.repository.SupportTicketRepository;
import in.mapmytour.customer.service.SupportTicketService;
import in.mapmytour.customer.service.SLAManagementService;
import in.mapmytour.customer.service.EscalationService;
import in.mapmytour.customer.service.BookingIntegrationService;
import in.mapmytour.customer.service.UserContextService;
import in.mapmytour.support.event.SupportEventProducer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final CustomerSupportAgentRepository agentRepository;
    
    @Autowired(required = false)
    private SupportEventProducer supportEventProducer;
    
    @Autowired(required = false)
    private SLAManagementService slaManagementService;
    
    @Autowired(required = false)
    private EscalationService escalationService;
    
    @Autowired(required = false)
    @SuppressWarnings("unused") // Optional dependency for future booking integration features
    private BookingIntegrationService bookingIntegrationService;
    
    @Autowired(required = false)
    @SuppressWarnings("unused") // Optional dependency for future notification features
    private in.mapmytour.customer.service.NotificationService notificationService;

    @Autowired(required = false)
    private UserContextService userContextService;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        try {
            // Validate customer ID is set
            if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            
            log.debug("Creating ticket for customer: {}", request.getCustomerId());

            SupportTicket ticket = SupportTicket.builder()
                    .id(UUID.randomUUID().toString())
                    .customerId(request.getCustomerId())
                    .subject(request.getSubject().trim())
                    .description(request.getDescription().trim())
                    .priority(request.getPriority())
                    .status(SupportTicket.TicketStatus.OPEN)
                    .category(request.getCategory())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .language(request.getLanguage() != null ? request.getLanguage() : "en")
                    .bookingId(request.getBookingId())
                    .bookingReference(request.getBookingReference())
                    .escalationLevel(0)
                    .build();

            final CustomerSupportAgent[] assignedAgent = new CustomerSupportAgent[1];
            // Auto-assign to available agent if possible
            agentRepository.findAvailableAgents().stream()
                    .findFirst()
                    .ifPresent(agent -> {
                        ticket.setAssignedAgentId(agent.getId());
                        ticket.setStatus(SupportTicket.TicketStatus.IN_PROGRESS);
                        assignedAgent[0] = agent;
                        log.info("Auto-assigned ticket to agent: {}", agent.getId());
                    });

            SupportTicket savedTicket = ticketRepository.save(ticket);
            
            // Initialize SLA tracking
            if (slaManagementService != null) {
                slaManagementService.updateSLAMetrics(savedTicket);
                savedTicket = ticketRepository.save(savedTicket);
            }
            
            // Check for escalation
            if (escalationService != null) {
                escalationService.checkAndEscalate(savedTicket);
                savedTicket = ticketRepository.save(savedTicket);
            }
            
            // Trigger notifications
            String customerEmail = getCustomerEmail(savedTicket.getCustomerId());
            if (notificationService != null) {
                try {
                    notificationService.sendTicketCreatedNotification(
                        savedTicket.getId(), 
                        savedTicket.getCustomerId(), 
                        customerEmail, 
                        savedTicket.getSubject()
                    );
                } catch (Exception ex) {
                    log.warn("Failed to send ticket created notification: {}", ex.getMessage());
                }

                if (assignedAgent[0] != null) {
                    try {
                        notificationService.sendAgentAssignmentNotification(
                            savedTicket.getId(), 
                            assignedAgent[0].getId(), 
                            assignedAgent[0].getEmail(), 
                            savedTicket.getSubject()
                        );
                    } catch (Exception ex) {
                        log.warn("Failed to send agent assignment notification: {}", ex.getMessage());
                    }
                }
            }
            
            // Publish ticket created event to Kafka
            if (supportEventProducer != null) {
                try {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId == null) {
                        correlationId = UUID.randomUUID().toString();
                    }
                    supportEventProducer.publishTicketCreated(
                        savedTicket.getId(), 
                        savedTicket.getCustomerId(), 
                        savedTicket.getSubject(), 
                        savedTicket.getPriority().name(), 
                        correlationId
                    );
                } catch (Exception e) {
                    log.warn("Failed to publish ticket created event for ticket {}: {}", savedTicket.getId(), e.getMessage());
                }
            }
            
            log.info("Ticket created successfully with ID: {}", savedTicket.getId());

            return mapToTicketResponse(savedTicket);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating ticket", e);
            throw new ServiceException("Failed to create ticket due to data integrity violation");
        } catch (Exception e) {
            log.error("Unexpected error while creating ticket", e);
            throw new ServiceException("Failed to create ticket: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(String id) {
        try {
            log.debug("Fetching ticket with ID: {}", id);

            SupportTicket ticket = ticketRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

            return mapToTicketResponse(ticket);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching ticket with ID: {}", id, e);
            throw new ServiceException("Failed to fetch ticket: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getAllTickets(Pageable pageable, String status, String category, String priority) {
        try {
            log.debug("Fetching all tickets with filters - status: {}, category: {}, priority: {}",
                    status, category, priority);

            String statusStr = null;
            String categoryStr = null;
            String priorityStr = null;

            try {
                if (StringUtils.hasText(status)) {
                    // Validate enum value
                    SupportTicket.TicketStatus.valueOf(status.toUpperCase());
                    statusStr = status.toUpperCase();
                }
                if (StringUtils.hasText(category)) {
                    // Validate enum value
                    SupportTicket.TicketCategory.valueOf(category.toUpperCase());
                    categoryStr = category.toUpperCase();
                }
                if (StringUtils.hasText(priority)) {
                    // Validate enum value
                    SupportTicket.TicketPriority.valueOf(priority.toUpperCase());
                    priorityStr = priority.toUpperCase();
                }
            } catch (IllegalArgumentException e) {
                throw new ServiceException("Invalid filter value provided");
            }

            // For native queries, we need to handle sorting differently
            // Create pageable without sort to avoid Spring Data JPA adding camelCase sorting
            Pageable pageableWithoutSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
            );
            return ticketRepository.searchTickets(null, statusStr, categoryStr, priorityStr, pageableWithoutSort)
                    .map(this::mapToTicketResponse);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching all tickets", e);
            throw new ServiceException("Failed to fetch tickets: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(String id, UpdateTicketRequest request) {
        try {
            log.debug("Updating ticket with ID: {}", id);

            SupportTicket ticket = ticketRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

            // Update fields if provided
            if (StringUtils.hasText(request.getSubject())) {
                ticket.setSubject(request.getSubject().trim());
            }
            if (StringUtils.hasText(request.getDescription())) {
                ticket.setDescription(request.getDescription().trim());
            }
            if (request.getPriority() != null) {
                ticket.setPriority(request.getPriority());
            }
            if (request.getCategory() != null) {
                ticket.setCategory(request.getCategory());
            }
            if (StringUtils.hasText(request.getAssignedAgentId())) {
                ticket.setAssignedAgentId(request.getAssignedAgentId());
            }
            if (StringUtils.hasText(request.getResolutionNotes())) {
                ticket.setResolutionNotes(request.getResolutionNotes().trim());
            }

            ticket.setUpdatedAt(LocalDateTime.now());

            SupportTicket updatedTicket = ticketRepository.save(ticket);
            
            // Trigger update notification
            if (notificationService != null) {
                try {
                    String customerEmail = getCustomerEmail(updatedTicket.getCustomerId());
                    notificationService.sendTicketUpdatedNotification(
                        updatedTicket.getId(), 
                        updatedTicket.getCustomerId(), 
                        customerEmail, 
                        "Your ticket details have been updated."
                    );
                } catch (Exception ex) {
                    log.warn("Failed to send ticket updated notification: {}", ex.getMessage());
                }
            }
            
            // Publish ticket updated event to Kafka
            if (supportEventProducer != null) {
                try {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId == null) {
                        correlationId = UUID.randomUUID().toString();
                    }
                    supportEventProducer.publishTicketUpdated(
                        updatedTicket.getId(), 
                        updatedTicket.getCustomerId(), 
                        updatedTicket.getStatus().name(), 
                        correlationId
                    );
                } catch (Exception e) {
                    log.warn("Failed to publish ticket updated event for ticket {}: {}", updatedTicket.getId(), e.getMessage());
                }
            }
            
            log.info("Ticket updated successfully with ID: {}", updatedTicket.getId());

            return mapToTicketResponse(updatedTicket);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating ticket with ID: {}", id, e);
            throw new ServiceException("Failed to update ticket: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TicketResponse updateTicketStatus(String id, TicketStatusUpdateRequest request) {
        try {
            log.debug("Updating ticket status for ID: {}", id);

            SupportTicket ticket = ticketRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

            try {
                SupportTicket.TicketStatus newStatus = SupportTicket.TicketStatus.valueOf(request.getStatus().toUpperCase());
                ticket.setStatus(newStatus);

                if (newStatus == SupportTicket.TicketStatus.RESOLVED ||
                        newStatus == SupportTicket.TicketStatus.CLOSED) {
                    ticket.setResolvedAt(LocalDateTime.now());
                }

                ticket.setUpdatedAt(LocalDateTime.now());
                
                // Update SLA metrics
                if (slaManagementService != null) {
                    slaManagementService.updateSLAMetrics(ticket);
                }

                SupportTicket updatedTicket = ticketRepository.save(ticket);
                
                // Trigger notification based on status
                if (notificationService != null) {
                    try {
                        String customerEmail = getCustomerEmail(updatedTicket.getCustomerId());
                        if (updatedTicket.getStatus() == SupportTicket.TicketStatus.RESOLVED ||
                            updatedTicket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
                            String notes = updatedTicket.getResolutionNotes() != null ? 
                                    updatedTicket.getResolutionNotes() : "No resolution notes provided.";
                            notificationService.sendTicketResolvedNotification(
                                updatedTicket.getId(), 
                                updatedTicket.getCustomerId(), 
                                customerEmail, 
                                "Your ticket has been marked as " + updatedTicket.getStatus().name() + ". Resolution: " + notes
                            );
                        } else {
                            notificationService.sendTicketUpdatedNotification(
                                updatedTicket.getId(), 
                                updatedTicket.getCustomerId(), 
                                customerEmail, 
                                "Your ticket status has been updated to: " + updatedTicket.getStatus().name()
                            );
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to send ticket status notification: {}", ex.getMessage());
                    }
                }

                // Publish ticket status update event to Kafka
                if (supportEventProducer != null) {
                    try {
                        String correlationId = MDC.get("correlationId");
                        if (correlationId == null) {
                            correlationId = UUID.randomUUID().toString();
                        }
                        supportEventProducer.publishTicketUpdated(
                            updatedTicket.getId(), 
                            updatedTicket.getCustomerId(), 
                            updatedTicket.getStatus().name(), 
                            correlationId
                        );
                        
                        // If resolved, also publish resolved event
                        if (updatedTicket.getStatus() == SupportTicket.TicketStatus.RESOLVED ||
                            updatedTicket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
                            supportEventProducer.publishTicketResolved(
                                updatedTicket.getId(), 
                                updatedTicket.getCustomerId(), 
                                correlationId
                            );
                        }
                    } catch (Exception e) {
                        log.warn("Failed to publish ticket status update event for ticket {}: {}", updatedTicket.getId(), e.getMessage());
                    }
                }
                
                log.info("Ticket status updated successfully for ID: {}", id);

                return mapToTicketResponse(updatedTicket);
            } catch (IllegalArgumentException e) {
                throw new ServiceException("Invalid ticket status: " + request.getStatus());
            }
        } catch (ResourceNotFoundException | ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating ticket status for ID: {}", id, e);
            throw new ServiceException("Failed to update ticket status: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketSummaryResponse> getTicketsByCustomer(String customerId, Pageable pageable) {
        try {
            log.debug("Fetching tickets for customer: {}", customerId);

            return ticketRepository.findByCustomerId(customerId, pageable)
                    .map(this::mapToTicketSummaryResponse);
        } catch (Exception e) {
            log.error("Error fetching tickets for customer: {}", customerId, e);
            throw new ServiceException("Failed to fetch tickets for customer: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(String id, String agentId) {
        try {
            log.debug("Assigning ticket {} to agent {}", id, agentId);

            SupportTicket ticket = ticketRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

            CustomerSupportAgent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));

            ticket.setAssignedAgentId(agentId);
            ticket.setStatus(SupportTicket.TicketStatus.IN_PROGRESS);
            ticket.setUpdatedAt(LocalDateTime.now());

            SupportTicket updatedTicket = ticketRepository.save(ticket);
            log.info("Ticket assigned successfully: {} to agent: {}", id, agentId);

            if (notificationService != null) {
                try {
                    notificationService.sendAgentAssignmentNotification(
                        updatedTicket.getId(), 
                        agent.getId(), 
                        agent.getEmail(), 
                        updatedTicket.getSubject()
                    );
                } catch (Exception ex) {
                    log.warn("Failed to send agent assignment notification: {}", ex.getMessage());
                }
            }

            return mapToTicketResponse(updatedTicket);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error assigning ticket: {}", id, e);
            throw new ServiceException("Failed to assign ticket: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> searchTickets(String query, Pageable pageable) {
        try {
            log.debug("Searching tickets with query: {}", query);

            if (!StringUtils.hasText(query)) {
                return getAllTickets(pageable, null, null, null);
            }

            return ticketRepository.searchTickets(query.trim(), null, null, null, pageable)
                    .map(this::mapToTicketResponse);
        } catch (Exception e) {
            log.error("Error searching tickets with query: {}", query, e);
            throw new ServiceException("Failed to search tickets: " + e.getMessage());
        }
    }

    private TicketResponse mapToTicketResponse(SupportTicket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .customerId(ticket.getCustomerId())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .category(ticket.getCategory())
                .assignedAgentId(ticket.getAssignedAgentId())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .resolutionNotes(ticket.getResolutionNotes())
                // SLA Management Fields
                .firstResponseAt(ticket.getFirstResponseAt())
                .responseTimeMinutes(ticket.getResponseTimeMinutes())
                .resolutionTimeMinutes(ticket.getResolutionTimeMinutes())
                .slaResponseTimeMinutes(ticket.getSlaResponseTimeMinutes())
                .slaResolutionTimeMinutes(ticket.getSlaResolutionTimeMinutes())
                .slaResponseMet(ticket.getSlaResponseMet())
                .slaResolutionMet(ticket.getSlaResolutionMet())
                // Booking System Integration
                .bookingId(ticket.getBookingId())
                .bookingReference(ticket.getBookingReference())
                // Multi-language Support
                .language(ticket.getLanguage())
                // Escalation Fields
                .escalationLevel(ticket.getEscalationLevel())
                .escalatedAt(ticket.getEscalatedAt())
                .escalationReason(ticket.getEscalationReason())
                .build();
    }

    private TicketSummaryResponse mapToTicketSummaryResponse(SupportTicket ticket) {
        return TicketSummaryResponse.builder()
                .id(ticket.getId())
                .subject(ticket.getSubject())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private String getCustomerEmail(String customerId) {
        if (userContextService != null) {
            String currentUserId = userContextService.getCurrentUserId();
            if (customerId.equals(currentUserId)) {
                String email = userContextService.getCurrentUserEmail();
                if (StringUtils.hasText(email)) {
                    return email;
                }
            }
        }
        return "customer-" + customerId + "@mapmytour.in";
    }
}