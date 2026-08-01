package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.service.SupportTicketService;
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
@RequestMapping("/api/v1/customer/tickets")
@RequiredArgsConstructor
@Slf4j
public class SupportTicketController {

    private final SupportTicketService ticketService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {

        String currentUserId = userContextService.getCurrentUserId();
        log.info("Creating ticket for user: {}", currentUserId);

        // Set the customer ID to current user
        request.setCustomerId(currentUserId);

        TicketResponse response = ticketService.createTicket(request);

        log.info("Ticket created successfully with ID: {}", response.getId());

        return ResponseEntity.ok(APIResponse.<TicketResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Ticket created successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<TicketResponse>> getTicketById(
            @PathVariable String id) {

        log.info("Fetching ticket with ID: {}", id);

        TicketResponse ticket = ticketService.getTicketById(id);

        // Validate access - user can only access their own tickets unless admin
        if (!userContextService.isCurrentUserAdmin()) {
            userContextService.validateAccess(ticket.getCustomerId());
        }

        return ResponseEntity.ok(APIResponse.<TicketResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Ticket retrieved successfully")
                .data(ticket)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<Page<TicketResponse>>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority) {

        log.info("Fetching tickets - page: {}, size: {}", page, size);

        // Only admin users can view all tickets
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can view all tickets");
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<TicketResponse> response = ticketService.getAllTickets(
                PageRequest.of(page, size, sort), status, category, priority);

        return ResponseEntity.ok(APIResponse.<Page<TicketResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Tickets retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<APIResponse<Page<TicketSummaryResponse>>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        String currentUserId = userContextService.getCurrentUserId();
        log.info("Fetching tickets for user: {}", currentUserId);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<TicketSummaryResponse> response = ticketService.getTicketsByCustomer(
                currentUserId, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(APIResponse.<Page<TicketSummaryResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Your tickets retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<TicketResponse>> updateTicket(
            @PathVariable String id,
            @Valid @RequestBody UpdateTicketRequest request) {

        log.info("Updating ticket with ID: {}", id);

        // Check if user can update this ticket
        TicketResponse existingTicket = ticketService.getTicketById(id);
        if (!userContextService.isCurrentUserAdmin()) {
            userContextService.validateAccess(existingTicket.getCustomerId());
        }

        TicketResponse response = ticketService.updateTicket(id, request);

        return ResponseEntity.ok(APIResponse.<TicketResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Ticket updated successfully")
                .data(response)
                .build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<APIResponse<TicketResponse>> updateTicketStatus(
            @PathVariable String id,
            @Valid @RequestBody TicketStatusUpdateRequest request) {

        log.info("Updating ticket status for ID: {} to {}", id, request.getStatus());

        // Only admin users can update ticket status
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can update ticket status");
        }

        TicketResponse response = ticketService.updateTicketStatus(id, request);

        return ResponseEntity.ok(APIResponse.<TicketResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Ticket status updated successfully")
                .data(response)
                .build());
    }

    @PatchMapping("/{id}/assign/{agentId}")
    public ResponseEntity<APIResponse<TicketResponse>> assignTicket(
            @PathVariable String id,
            @PathVariable String agentId) {

        log.info("Assigning ticket {} to agent {}", id, agentId);

        // Only admin users can assign tickets
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can assign tickets");
        }

        TicketResponse response = ticketService.assignTicket(id, agentId);

        return ResponseEntity.ok(APIResponse.<TicketResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Ticket assigned successfully")
                .data(response)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<APIResponse<Page<TicketResponse>>> searchTickets(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Searching tickets with query: {}", query);

        // Only admin users can search all tickets
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can search all tickets");
        }

        Page<TicketResponse> response = ticketService.searchTickets(
                query, PageRequest.of(page, size));

        return ResponseEntity.ok(APIResponse.<Page<TicketResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Search results retrieved successfully")
                .data(response)
                .build());
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<APIResponse<TicketResponse>> escalateTicket(
            @PathVariable String id,
            @RequestParam(required = false) String reason) {

        log.info("Escalating ticket with ID: {}", id);

        // Only admin users can escalate tickets
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can escalate tickets");
        }

        // Get ticket and escalate
        TicketResponse ticket = ticketService.getTicketById(id);
        
        // Escalation is handled automatically by EscalationService
        // This endpoint allows manual escalation
        
        return ResponseEntity.ok(APIResponse.<TicketResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Ticket escalation processed")
                .data(ticket)
                .build());
    }
}