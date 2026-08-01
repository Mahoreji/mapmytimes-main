package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.entity.TicketConversation;
import in.mapmytour.customer.exception.ResourceNotFoundException;
import in.mapmytour.customer.exception.ServiceException;
import in.mapmytour.customer.repository.SupportTicketRepository;
import in.mapmytour.customer.repository.TicketConversationRepository;
import in.mapmytour.customer.service.TicketConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketConversationServiceImpl implements TicketConversationService {

    private final TicketConversationRepository conversationRepository;
    private final SupportTicketRepository ticketRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private in.mapmytour.customer.service.SLAManagementService slaManagementService;

    @Override
    @Transactional
    public ConversationResponse addConversation(CreateConversationRequest request) {
        try {
            log.debug("Adding conversation to ticket: {}", request.getTicketId());

            // Validate ticket exists
            if (!ticketRepository.existsById(request.getTicketId())) {
                throw new ResourceNotFoundException("Ticket not found with id: " + request.getTicketId());
            }

            TicketConversation conversation = TicketConversation.builder()
                    .id(UUID.randomUUID().toString())
                    .ticketId(request.getTicketId())
                    .senderId(request.getSenderId())
                    .senderType(determineSenderType(request.getSenderId(), request.getTicketId()))
                    .message(request.getMessage().trim())
                    .attachmentUrl(request.getAttachmentUrl())
                    .isInternalNote(request.isInternalNote())
                    .sentAt(LocalDateTime.now())
                    .build();

            TicketConversation savedConversation = conversationRepository.save(conversation);

            // Update ticket's updatedAt timestamp and track first response for SLA
            ticketRepository.findById(request.getTicketId()).ifPresent(ticket -> {
                ticket.setUpdatedAt(LocalDateTime.now());
                
                // Record first response time if this is the first agent response
                if (ticket.getFirstResponseAt() == null && 
                    (savedConversation.getSenderType() == TicketConversation.SenderType.SUPPORT_AGENT ||
                     savedConversation.getSenderType() == TicketConversation.SenderType.AGENT)) {
                    ticket.setFirstResponseAt(LocalDateTime.now());
                    
                    // Immediately update SLA metrics with first response time
                    if (slaManagementService != null) {
                        try {
                            slaManagementService.updateSLAMetrics(ticket);
                        } catch (Exception ex) {
                            log.warn("Failed to update SLA metrics on first response: {}", ex.getMessage());
                        }
                    }
                }
                
                ticketRepository.save(ticket);
            });

            log.info("Conversation added successfully with ID: {}", savedConversation.getId());
            return mapToConversationResponse(savedConversation);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while adding conversation", e);
            throw new ServiceException("Failed to add conversation due to data integrity violation");
        } catch (Exception e) {
            log.error("Unexpected error while adding conversation", e);
            throw new ServiceException("Failed to add conversation: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversationsByTicket(String ticketId, Pageable pageable, boolean includeInternal) {
        try {
            log.debug("Fetching conversations for ticket: {}, includeInternal: {}", ticketId, includeInternal);

            if (!ticketRepository.existsById(ticketId)) {
                throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
            }

            return conversationRepository.findByTicketIdWithInternalFilter(ticketId, includeInternal, pageable)
                    .map(this::mapToConversationResponse);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching conversations for ticket: {}", ticketId, e);
            throw new ServiceException("Failed to fetch conversations: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ConversationResponse updateConversation(String conversationId, UpdateConversationRequest request) {
        try {
            log.debug("Updating conversation with ID: {}", conversationId);

            TicketConversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

            // Update fields if provided
            if (StringUtils.hasText(request.getMessage())) {
                conversation.setMessage(request.getMessage().trim());
            }

            if (request.getAttachmentUrl() != null) {
                conversation.setAttachmentUrl(request.getAttachmentUrl());
            }

            if (request.getIsInternalNote() != null) {
                conversation.setInternalNote(request.getIsInternalNote());
            }

            TicketConversation updatedConversation = conversationRepository.save(conversation);
            log.info("Conversation updated successfully with ID: {}", updatedConversation.getId());

            return mapToConversationResponse(updatedConversation);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating conversation with ID: {}", conversationId, e);
            throw new ServiceException("Failed to update conversation: " + e.getMessage());
        }
    }

    private TicketConversation.SenderType determineSenderType(String senderId, String ticketId) {
        return ticketRepository.findById(ticketId)
                .map(ticket -> {
                    if (ticket.getCustomerId().equals(senderId)) {
                        return TicketConversation.SenderType.CUSTOMER;
                    } else if (ticket.getAssignedAgentId() != null &&
                            ticket.getAssignedAgentId().equals(senderId)) {
                        return TicketConversation.SenderType.SUPPORT_AGENT;
                    }
                    return TicketConversation.SenderType.SYSTEM;
                })
                .orElse(TicketConversation.SenderType.SYSTEM);
    }

    private ConversationResponse mapToConversationResponse(TicketConversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .ticketId(conversation.getTicketId())
                .senderId(conversation.getSenderId())
                .senderType(conversation.getSenderType())
                .message(conversation.getMessage())
                .attachmentUrl(conversation.getAttachmentUrl())
                .isInternalNote(conversation.isInternalNote())
                .sentAt(conversation.getSentAt())
                .build();
    }
}