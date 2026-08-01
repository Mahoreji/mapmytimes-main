package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CustomerSupportAgentService {
    AgentResponse createAgent(CreateAgentRequest request);
    AgentResponse getAgentById(String id);
    Page<AgentSummaryResponse> getAllAgents(Pageable pageable, boolean activeOnly);
    AgentResponse updateAgent(String id, UpdateAgentRequest request);
    void deleteAgent(String id);
    List<AgentSummaryResponse> getAvailableAgents();
    Page<AgentSummaryResponse> searchAgents(String searchTerm, Pageable pageable);
    AgentResponse getAgentByEmail(String email);
    void deactivateAgent(String id);
    void activateAgent(String id);
}