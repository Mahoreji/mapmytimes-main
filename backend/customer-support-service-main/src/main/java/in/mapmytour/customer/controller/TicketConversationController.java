package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.service.TicketConversationService;
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
@RequestMapping("/api/v1/customer/conversations")
@RequiredArgsConstructor
@Slf4j
public class TicketConversationController {

    private final TicketConversationService conversationService;
    private final SupportTicketService ticketService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<ConversationResponse>> addConversation(
            @Valid @RequestBody CreateConversationRequest request) {

        String currentUserId = userContextService.getCurrentUserId();
        log.info("Adding conversation for ticket: {} by user: {}", request.getTicketId(), currentUserId);

        // Validate user can add conversation to this ticket
        TicketResponse ticket = ticketService.getTicketById(request.getTicketId());
        if (!userContextService.isCurrentUserAdmin()) {
            userContextService.validateAccess(ticket.getCustomerId());
        }

        // Set the sender ID to current user
        request.setSenderId(currentUserId);

        ConversationResponse response = conversationService.addConversation(request);

        log.info("Conversation added successfully with ID: {}", response.getId());

        return ResponseEntity.ok(APIResponse.<ConversationResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Conversation added successfully")
                .data(response)
                .build());
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<APIResponse<Page<ConversationResponse>>> getConversationsByTicket(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sentAt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            @RequestParam(defaultValue = "false") boolean includeInternal) {

        log.info("Fetching conversations for ticket: {}", ticketId);

        // Validate user can access this ticket's conversations
        TicketResponse ticket = ticketService.getTicketById(ticketId);
        if (!userContextService.isCurrentUserAdmin()) {
            userContextService.validateAccess(ticket.getCustomerId());
            includeInternal = false; // Regular users can't see internal notes
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<ConversationResponse> response = conversationService.getConversationsByTicket(
                ticketId, PageRequest.of(page, size, sort), includeInternal);

        return ResponseEntity.ok(APIResponse.<Page<ConversationResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Conversations retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{conversationId}")
    public ResponseEntity<APIResponse<ConversationResponse>> updateConversation(
            @PathVariable String conversationId,
            @Valid @RequestBody UpdateConversationRequest request) {

        log.info("Updating conversation with ID: {}", conversationId);

        // Only admin users can update conversations
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can update conversations");
        }

        ConversationResponse response = conversationService.updateConversation(conversationId, request);

        return ResponseEntity.ok(APIResponse.<ConversationResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Conversation updated successfully")
                .data(response)
                .build());
    }
}