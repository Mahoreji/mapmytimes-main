package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.PaginationRequest;
import in.mapmytour.blog.dto.request.postcomment.CreateCommentRequest;
import in.mapmytour.blog.dto.request.postcomment.UpdateCommentRequest;
import in.mapmytour.blog.dto.response.postcomment.PostCommentResponse;
import in.mapmytour.blog.dto.response.PaginatedResponse;
import in.mapmytour.blog.service.PostCommentService;
import in.mapmytour.blog.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blog/comments")
@RequiredArgsConstructor
@Slf4j
public class PostCommentController {

    private final PostCommentService postCommentService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<PostCommentResponse>> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest) {

        log.debug("PostCommentController.createComment called for request: {}", httpRequest.getRequestURI());
        String userId = userContextService.getCurrentUserId(httpRequest);
        log.debug("Comment creation - extracted userId: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.<PostCommentResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.UNAUTHORIZED.value())
                            .message("User ID is required")
                            .data(null)
                            .build());
        }
        
        request.setUserId(userId);

        PostCommentResponse response = postCommentService.createComment(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<PostCommentResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Comment created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<PaginatedResponse<PostCommentResponse>>> getAllComments(
            @Valid @ModelAttribute PaginationRequest paginationRequest) {

        PaginatedResponse<PostCommentResponse> response = postCommentService.getAllComments(paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<PostCommentResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Comments retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<APIResponse<PostCommentResponse>> getComment(@PathVariable String commentId) {
        PostCommentResponse response = postCommentService.getComment(commentId);

        return ResponseEntity.ok(APIResponse.<PostCommentResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Comment retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<APIResponse<List<PostCommentResponse>>> getPostComments(@PathVariable String postId) {
        List<PostCommentResponse> response = postCommentService.getPostComments(postId);

        return ResponseEntity.ok(APIResponse.<List<PostCommentResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Post comments retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/post/{postId}/approved")
    public ResponseEntity<APIResponse<List<PostCommentResponse>>> getApprovedPostComments(@PathVariable String postId) {
        List<PostCommentResponse> response = postCommentService.getApprovedPostComments(postId);

        return ResponseEntity.ok(APIResponse.<List<PostCommentResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Approved post comments retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/pending")
    public ResponseEntity<APIResponse<PaginatedResponse<PostCommentResponse>>> getPendingComments(
            @Valid @ModelAttribute PaginationRequest paginationRequest) {

        PaginatedResponse<PostCommentResponse> response = postCommentService.getPendingComments(paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<PostCommentResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Pending comments retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<APIResponse<PaginatedResponse<PostCommentResponse>>> getUserComments(
            @PathVariable String userId,
            @Valid @ModelAttribute PaginationRequest paginationRequest) {

        PaginatedResponse<PostCommentResponse> response = postCommentService.getUserComments(userId, paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<PostCommentResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("User comments retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/my-comments")
    public ResponseEntity<APIResponse<PaginatedResponse<PostCommentResponse>>> getMyComments(
            @Valid @ModelAttribute PaginationRequest paginationRequest,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        PaginatedResponse<PostCommentResponse> response = postCommentService.getUserComments(userId, paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<PostCommentResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("My comments retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<APIResponse<PostCommentResponse>> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        PostCommentResponse response = postCommentService.updateComment(commentId, request, userId);

        return ResponseEntity.ok(APIResponse.<PostCommentResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Comment updated successfully")
                .data(response)
                .build());
    }

    @PostMapping("/{commentId}/approve")
    public ResponseEntity<APIResponse<PostCommentResponse>> approveComment(
            @PathVariable String commentId,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<PostCommentResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        PostCommentResponse response = postCommentService.approveComment(commentId);

        return ResponseEntity.ok(APIResponse.<PostCommentResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Comment approved successfully")
                .data(response)
                .build());
    }

    @PostMapping("/{commentId}/reject")
    public ResponseEntity<APIResponse<PostCommentResponse>> rejectComment(
            @PathVariable String commentId,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<PostCommentResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        PostCommentResponse response = postCommentService.rejectComment(commentId);

        return ResponseEntity.ok(APIResponse.<PostCommentResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Comment rejected successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<APIResponse<Void>> deleteComment(
            @PathVariable String commentId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        boolean isAdmin = userContextService.isCurrentUserAdmin(httpRequest);

        postCommentService.deleteComment(commentId, userId, isAdmin);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Comment deleted successfully")
                .build());
    }
}