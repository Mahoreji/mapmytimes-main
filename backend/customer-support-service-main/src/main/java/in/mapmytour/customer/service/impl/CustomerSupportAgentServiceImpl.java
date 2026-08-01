package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.entity.CustomerSupportAgent;
import in.mapmytour.customer.exception.ResourceNotFoundException;
import in.mapmytour.customer.exception.ServiceException;
import in.mapmytour.customer.repository.CustomerSupportAgentRepository;
import in.mapmytour.customer.service.CustomerSupportAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerSupportAgentServiceImpl implements CustomerSupportAgentService {

    private final CustomerSupportAgentRepository agentRepository;

    @Override
    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request) {
        try {
            log.debug("Creating agent with email: {}", request.getEmail());

            // Check if agent with same email already exists
            if (agentRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new ServiceException("Agent with email '" + request.getEmail() + "' already exists");
            }

            // Check if agent with same userId already exists
            if (agentRepository.existsByUserId(request.getUserId())) {
                throw new ServiceException("Agent with user ID '" + request.getUserId() + "' already exists");
            }

            CustomerSupportAgent agent = CustomerSupportAgent.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(request.getUserId())
                    .fullName(request.getFullName().trim())
                    .email(request.getEmail().trim().toLowerCase())
                    .skills(request.getSkills())
                    .maxActiveTickets(request.getMaxActiveTickets())
                    .isActive(true)
                    .build();

            CustomerSupportAgent savedAgent = agentRepository.save(agent);
            log.info("Agent created successfully with ID: {}", savedAgent.getId());

            return mapToAgentResponse(savedAgent);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating agent", e);
            throw new ServiceException("Failed to create agent due to data integrity violation");
        } catch (Exception e) {
            log.error("Unexpected error while creating agent", e);
            throw new ServiceException("Failed to create agent: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AgentResponse getAgentById(String id) {
        try {
            log.debug("Fetching agent with ID: {}", id);

            CustomerSupportAgent agent = agentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));

            return mapToAgentResponse(agent);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching agent with ID: {}", id, e);
            throw new ServiceException("Failed to fetch agent: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgentSummaryResponse> getAllAgents(Pageable pageable, boolean activeOnly) {
        try {
            log.debug("Fetching all agents, activeOnly: {}", activeOnly);

            if (activeOnly) {
                return agentRepository.findByIsActive(true, pageable)
                        .map(this::mapToAgentSummaryResponse);
            } else {
                return agentRepository.findAll(pageable)
                        .map(this::mapToAgentSummaryResponse);
            }
        } catch (Exception e) {
            log.error("Error fetching all agents", e);
            throw new ServiceException("Failed to fetch agents: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public AgentResponse updateAgent(String id, UpdateAgentRequest request) {
        try {
            log.debug("Updating agent with ID: {}", id);

            CustomerSupportAgent agent = agentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));

            // Update fields if provided
            if (StringUtils.hasText(request.getFullName())) {
                agent.setFullName(request.getFullName().trim());
            }

            if (request.getSkills() != null) {
                agent.setSkills(request.getSkills());
            }

            if (request.getIsActive() != null) {
                agent.setActive(request.getIsActive());
            }

            if (request.getMaxActiveTickets() != null) {
                agent.setMaxActiveTickets(request.getMaxActiveTickets());
            }

            CustomerSupportAgent updatedAgent = agentRepository.save(agent);
            log.info("Agent updated successfully with ID: {}", updatedAgent.getId());

            return mapToAgentResponse(updatedAgent);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating agent", e);
            throw new ServiceException("Failed to update agent due to data integrity violation");
        } catch (Exception e) {
            log.error("Error updating agent with ID: {}", id, e);
            throw new ServiceException("Failed to update agent: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteAgent(String id) {
        try {
            log.debug("Deleting agent with ID: {}", id);

            if (!agentRepository.existsById(id)) {
                throw new ResourceNotFoundException("Agent not found with id: " + id);
            }

            agentRepository.deleteById(id);
            log.info("Agent deleted successfully with ID: {}", id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting agent with ID: {}", id, e);
            throw new ServiceException("Failed to delete agent: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentSummaryResponse> getAvailableAgents() {
        try {
            log.debug("Fetching available agents");

            return agentRepository.findAvailableAgents()
                    .stream()
                    .map(this::mapToAgentSummaryResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching available agents", e);
            throw new ServiceException("Failed to fetch available agents: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgentSummaryResponse> searchAgents(String searchTerm, Pageable pageable) {
        try {
            log.debug("Searching agents with term: {}", searchTerm);

            if (!StringUtils.hasText(searchTerm)) {
                return getAllAgents(pageable, false);
            }

            return agentRepository.searchAgents(searchTerm.trim(), pageable)
                    .map(this::mapToAgentSummaryResponse);
        } catch (Exception e) {
            log.error("Error searching agents with term: {}", searchTerm, e);
            throw new ServiceException("Failed to search agents: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AgentResponse getAgentByEmail(String email) {
        try {
            log.debug("Fetching agent with email: {}", email);

            CustomerSupportAgent agent = agentRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with email: " + email));

            return mapToAgentResponse(agent);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching agent with email: {}", email, e);
            throw new ServiceException("Failed to fetch agent: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deactivateAgent(String id) {
        try {
            log.debug("Deactivating agent with ID: {}", id);

            CustomerSupportAgent agent = agentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));

            agent.setActive(false);
            agentRepository.save(agent);
            log.info("Agent deactivated successfully with ID: {}", id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deactivating agent with ID: {}", id, e);
            throw new ServiceException("Failed to deactivate agent: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void activateAgent(String id) {
        try {
            log.debug("Activating agent with ID: {}", id);

            CustomerSupportAgent agent = agentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));

            agent.setActive(true);
            agentRepository.save(agent);
            log.info("Agent activated successfully with ID: {}", id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error activating agent with ID: {}", id, e);
            throw new ServiceException("Failed to activate agent: " + e.getMessage());
        }
    }

    private AgentResponse mapToAgentResponse(CustomerSupportAgent agent) {
        return AgentResponse.builder()
                .id(agent.getId())
                .userId(agent.getUserId())
                .fullName(agent.getFullName())
                .email(agent.getEmail())
                .skills(agent.getSkills())
                .isActive(agent.isActive())
                .maxActiveTickets(agent.getMaxActiveTickets())
                .build();
    }

    private AgentSummaryResponse mapToAgentSummaryResponse(CustomerSupportAgent agent) {
        return AgentSummaryResponse.builder()
                .id(agent.getId())
                .fullName(agent.getFullName())
                .email(agent.getEmail())
                .isActive(agent.isActive())
                .build();
    }
}