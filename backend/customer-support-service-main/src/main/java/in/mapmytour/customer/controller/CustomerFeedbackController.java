package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.service.CustomerFeedbackService;
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
@RequestMapping("/api/v1/customer/feedback")
@RequiredArgsConstructor
@Slf4j
public class CustomerFeedbackController {

    private final CustomerFeedbackService feedbackService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<FeedbackResponse>> submitFeedback(
            @Valid @RequestBody SubmitFeedbackRequest request) {

        String currentUserId = userContextService.getCurrentUserId();
        log.info("Submitting feedback for user: {}", currentUserId);

        // Set the customer ID to current user
        request.setCustomerId(currentUserId);

        FeedbackResponse response = feedbackService.submitFeedback(request);

        log.info("Feedback submitted successfully with ID: {}", response.getId());

        return ResponseEntity.ok(APIResponse.<FeedbackResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Feedback submitted successfully")
                .data(response)
                .build());
    }

    @GetMapping("/my-feedback")
    public ResponseEntity<APIResponse<Page<FeedbackResponse>>> getMyFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        String currentUserId = userContextService.getCurrentUserId();
        log.info("Fetching feedback for user: {}", currentUserId);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<FeedbackResponse> response = feedbackService.getFeedbackByCustomer(
                currentUserId, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(APIResponse.<Page<FeedbackResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Your feedback retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/stats")
    public ResponseEntity<APIResponse<FeedbackStatsResponse>> getFeedbackStats() {
        log.info("Fetching feedback statistics");

        // Only admin users can view feedback stats
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can view feedback statistics");
        }

        FeedbackStatsResponse response = feedbackService.getFeedbackStats();

        return ResponseEntity.ok(APIResponse.<FeedbackStatsResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Feedback statistics retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<FeedbackResponse>> getFeedbackById(
            @PathVariable String id) {
        log.info("Fetching feedback with ID: {}", id);

        FeedbackResponse response = feedbackService.getFeedbackById(id);

        // Validate access - user can only access their own feedback unless admin
        if (!userContextService.isCurrentUserAdmin()) {
            String currentUserId = userContextService.getCurrentUserId();
            if (response.getCustomerId() != null && !response.getCustomerId().equals(currentUserId)) {
                throw new AccessDeniedException("Access denied: You can only access your own feedback");
            }
        }

        return ResponseEntity.ok(APIResponse.<FeedbackResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Feedback retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<Page<FeedbackResponse>>> getAllFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating) {

        log.info("Fetching all feedback - page: {}, size: {}", page, size);

        // Only admin users can view all feedback
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can view all feedback");
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<FeedbackResponse> response = feedbackService.getAllFeedback(
                PageRequest.of(page, size, sort), minRating, maxRating);

        return ResponseEntity.ok(APIResponse.<Page<FeedbackResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Feedback retrieved successfully")
                .data(response)
                .build());
    }
}