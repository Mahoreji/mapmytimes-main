package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.service.QuoteRequestService;
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
@RequestMapping("/api/v1/customer/quote-requests")
@RequiredArgsConstructor
@Slf4j
public class QuoteRequestController {

    private final QuoteRequestService quoteRequestService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<QuoteResponseDTO>> createQuoteRequest(
            @Valid @RequestBody QuoteRequestDTO request) {

        String currentUserEmail = userContextService.getCurrentUserEmail();
        log.info("Creating quote request for user: {}", currentUserEmail);

        // Set the email to current user's email
        request.getPersonalInfo().setEmail(currentUserEmail);

        QuoteResponseDTO response = quoteRequestService.createQuoteRequest(request);

        log.info("Quote request created successfully with ID: {}", response.getId());

        return ResponseEntity.ok(APIResponse.<QuoteResponseDTO>builder()
                .success(true)
                .statusCode(201)
                .message("Quote request created successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<QuoteResponseDTO>> getQuoteRequestById(
            @PathVariable String id) {

        log.info("Fetching quote request with ID: {}", id);

        QuoteResponseDTO quote = quoteRequestService.getQuoteRequestById(id);

        // Validate access - user can only access their own quotes unless admin
        if (!userContextService.isCurrentUserAdmin()) {
            String currentUserEmail = userContextService.getCurrentUserEmail();
            if (!currentUserEmail.equals(quote.getPersonalInfo().getEmail())) {
                throw new AccessDeniedException(
                        "Access denied: You can only access your own quote requests");
            }
        }

        return ResponseEntity.ok(APIResponse.<QuoteResponseDTO>builder()
                .success(true)
                .statusCode(200)
                .message("Quote request retrieved successfully")
                .data(quote)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<Page<QuoteSummaryDTO>>> getAllQuoteRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String status) {

        log.info("Fetching quote requests - page: {}, size: {}", page, size);

        // Only admin users can view all quote requests
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can view all quote requests");
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<QuoteSummaryDTO> response = quoteRequestService.getAllQuoteRequests(
                PageRequest.of(page, size, sort), status);

        return ResponseEntity.ok(APIResponse.<Page<QuoteSummaryDTO>>builder()
                .success(true)
                .statusCode(200)
                .message("Quote requests retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/my-quotes")
    public ResponseEntity<APIResponse<Page<QuoteSummaryDTO>>> getMyQuoteRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        String currentUserEmail = userContextService.getCurrentUserEmail();
        log.info("Fetching quote requests for user: {}", currentUserEmail);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<QuoteSummaryDTO> response = quoteRequestService.getQuoteRequestsByCustomer(
                currentUserEmail, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(APIResponse.<Page<QuoteSummaryDTO>>builder()
                .success(true)
                .statusCode(200)
                .message("Your quote requests retrieved successfully")
                .data(response)
                .build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<APIResponse<QuoteResponseDTO>> updateQuoteStatus(
            @PathVariable String id,
            @Valid @RequestBody QuoteStatusUpdateDTO updateDTO) {

        log.info("Updating quote status for ID: {} to {}", id, updateDTO.getStatus());

        // Only admin users can update quote status
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can update quote status");
        }

        QuoteResponseDTO response = quoteRequestService.updateQuoteStatus(id, updateDTO);

        return ResponseEntity.ok(APIResponse.<QuoteResponseDTO>builder()
                .success(true)
                .statusCode(200)
                .message("Quote request status updated successfully")
                .data(response)
                .build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<APIResponse<Page<QuoteSummaryDTO>>> getQuoteRequestsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching quote requests by status: {}", status);

        // Only admin users can filter by status
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can filter quote requests by status");
        }

        Page<QuoteSummaryDTO> response = quoteRequestService.getQuoteRequestsByStatus(
                status, PageRequest.of(page, size));

        return ResponseEntity.ok(APIResponse.<Page<QuoteSummaryDTO>>builder()
                .success(true)
                .statusCode(200)
                .message("Quote requests by status retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<APIResponse<Page<QuoteSummaryDTO>>> searchQuoteRequests(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Searching quote requests with query: {}", query);

        // Only admin users can search all quotes
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can search all quote requests");
        }

        Page<QuoteSummaryDTO> response = quoteRequestService.searchQuoteRequests(
                query, PageRequest.of(page, size));

        return ResponseEntity.ok(APIResponse.<Page<QuoteSummaryDTO>>builder()
                .success(true)
                .statusCode(200)
                .message("Search results retrieved successfully")
                .data(response)
                .build());
    }

    @PatchMapping("/{quoteId}/assign/{agentId}")
    public ResponseEntity<APIResponse<QuoteResponseDTO>> assignAgentToQuote(
            @PathVariable String quoteId,
            @PathVariable String agentId) {

        log.info("Assigning agent {} to quote {}", agentId, quoteId);

        // Only admin users can assign agents
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can assign agents to quote requests");
        }

        QuoteResponseDTO response = quoteRequestService.assignAgentToQuote(quoteId, agentId);

        return ResponseEntity.ok(APIResponse.<QuoteResponseDTO>builder()
                .success(true)
                .statusCode(200)
                .message("Agent assigned to quote request successfully")
                .data(response)
                .build());
    }
}