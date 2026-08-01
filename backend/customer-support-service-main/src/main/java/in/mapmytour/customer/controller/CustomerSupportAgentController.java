package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.service.CustomerSupportAgentService;
import in.mapmytour.customer.service.UserContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/agents")
@RequiredArgsConstructor
@Slf4j
public class CustomerSupportAgentController {

    private final CustomerSupportAgentService agentService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<AgentResponse>> createAgent(
            @Valid @RequestBody CreateAgentRequest request) {

        log.info("Creating agent: {}", request.getEmail());

        // Only admin users can create agents
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can create support agents");
        }

        AgentResponse response = agentService.createAgent(request);

        log.info("Agent created successfully with ID: {}", response.getId());

        return ResponseEntity.ok(APIResponse.<AgentResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Agent created successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<AgentResponse>> getAgentById(@PathVariable String id) {
        log.info("Fetching agent with ID: {}", id);

        // Only admin users can view agent details
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can view agent details");
        }

        AgentResponse response = agentService.getAgentById(id);

        return ResponseEntity.ok(APIResponse.<AgentResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Agent retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<Page<AgentSummaryResponse>>> getAllAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        log.info("Fetching agents - page: {}, size: {}, activeOnly: {}", page, size, activeOnly);

        // Only admin users can view all agents
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can view agent list");
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<AgentSummaryResponse> response = agentService.getAllAgents(
                PageRequest.of(page, size, sort), activeOnly);

        return ResponseEntity.ok(APIResponse.<Page<AgentSummaryResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Agents retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<AgentResponse>> updateAgent(
            @PathVariable String id,
            @Valid @RequestBody UpdateAgentRequest request) {

        log.info("Updating agent with ID: {}", id);

        // Only admin users can update agents
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can update agents");
        }

        AgentResponse response = agentService.updateAgent(id, request);

        return ResponseEntity.ok(APIResponse.<AgentResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Agent updated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteAgent(@PathVariable String id) {
        log.info("Deleting agent with ID: {}", id);

        // Only admin users can delete agents
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can delete agents");
        }

        agentService.deleteAgent(id);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Agent deleted successfully")
                .build());
    }

    @GetMapping("/available")
    public ResponseEntity<APIResponse<Object>> getAvailableAgents() {
        log.info("Fetching available agents");

        // Only admin users can view available agents
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can view available agents");
        }

        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .statusCode(200)
                .message("Available agents retrieved successfully")
                .data(agentService.getAvailableAgents())
                .build());
    }
}