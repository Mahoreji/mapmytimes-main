package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.PaginationRequest;
import in.mapmytour.blog.dto.request.postcomment.CreateCommentRequest;
import in.mapmytour.blog.dto.request.postcomment.UpdateCommentRequest;
import in.mapmytour.blog.dto.response.PaginatedResponse;
import in.mapmytour.blog.dto.response.postcomment.PostCommentResponse;
import in.mapmytour.blog.entity.BlogPost;
import in.mapmytour.blog.entity.PostComment;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.exception.ForbiddenException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.repository.BlogPostRepository;
import in.mapmytour.blog.repository.PostCommentRepository;
import in.mapmytour.blog.service.PostCommentService;
import in.mapmytour.blog.utils.BlogMapper;
import in.mapmytour.blog.client.AuthServiceClient;
import in.mapmytour.blog.dto.external.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostCommentServiceImpl implements PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogMapper blogMapper;
    private final AuthServiceClient authServiceClient;

    @Override
    public PostCommentResponse createComment(CreateCommentRequest request) {
        log.info("Creating comment for post: {} by user: {}", request.getPostId(), request.getUserId());

        // Validate blog post exists
        BlogPost blogPost = blogPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + request.getPostId()));

        // Check if comments are allowed on this post
        if (!blogPost.getAllowComments()) {
            throw new BadRequestException("Comments are not allowed on this blog post");
        }

        // Validate parent comment if provided
        if (StringUtils.hasText(request.getParentCommentId())) {
            PostComment parentComment = postCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found with ID: " + request.getParentCommentId()));

            // Ensure parent comment belongs to the same post
            if (!parentComment.getPost().getId().equals(request.getPostId())) {
                throw new BadRequestException("Parent comment does not belong to the specified post");
            }
        }

        try {
            // Fetch author metadata from auth-service if userId is present
            String authorEmail = null;
            String authorFirstName = null;
            String authorLastName = null;
            String authorAvatarUrl = null;
            UserProfileResponse profile = null;

            if (StringUtils.hasText(request.getUserId())) {
                log.debug("Fetching author profile for comment creator: {}", request.getUserId());
                try {
                    profile = authServiceClient.getUserProfile(request.getUserId());
                    if (profile != null) {
                        authorEmail = profile.getEmail();
                        authorFirstName = profile.getFirstName();
                        authorLastName = profile.getLastName();
                        authorAvatarUrl = profile.getAvatarUrl();
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch user profile for comment creator: {}", request.getUserId());
                }
            }

            // Auto-approve logic:
            // 1. If post type is NOT 'BLOG' (e.g. SOCIAL, STORY), no approval needed.
            // 2. If commenter is the post author, no approval needed even for BLOG.
            String status = "PENDING";
            String postType = blogPost.getPostType() != null ? blogPost.getPostType() : "BLOG";
            
            if (!"BLOG".equalsIgnoreCase(postType) || request.getUserId().equals(blogPost.getUserId())) {
                log.info("Auto-approving comment. PostType: {}, IsAuthor: {}", 
                        postType, request.getUserId().equals(blogPost.getUserId()));
                status = "APPROVED";
            }

            PostComment comment = PostComment.builder()
                    .post(blogPost)
                    .content(request.getContent())
                    .userId(request.getUserId())
                    .parentCommentId(request.getParentCommentId())
                    .authorEmail(authorEmail)
                    .authorFirstName(authorFirstName)
                    .authorLastName(authorLastName)
                    .authorAvatarUrl(authorAvatarUrl)
                    .status(status)
                    .build();

            PostComment savedComment = postCommentRepository.save(comment);
            log.info("Comment created successfully with ID: {}", savedComment.getId());

            // Send notification to post author
            if (!request.getUserId().equals(blogPost.getUserId())) {
                String message = (authorFirstName != null ? authorFirstName : "Someone") + " commented on your post";
                authServiceClient.sendNotification(
                        blogPost.getUserId(),
                        request.getUserId(),
                        "SOCIAL_COMMENT",
                        message,
                        blogPost.getId()
                );
            }

            return blogMapper.toPostCommentResponse(savedComment, profile);
        } catch (Exception e) {
            log.error("Error creating comment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create comment: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PostCommentResponse getComment(String commentId) {
        log.info("Fetching comment with ID: {}", commentId);

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        // Fetch profile for comment author
        UserProfileResponse profile = null;
        try {
            profile = authServiceClient.getUserProfile(comment.getUserId());
        } catch (Exception e) {
            log.warn("Failed to fetch user profile for comment author: {}", comment.getUserId());
        }

        PostCommentResponse response = blogMapper.toPostCommentResponse(comment, profile);

        // Load and enrich replies
        List<PostComment> replies = postCommentRepository.findRepliesByParentId(commentId);
        if (!replies.isEmpty()) {
            Map<String, UserProfileResponse> profileMap = fetchProfilesForComments(replies);
            response.setReplies(replies.stream()
                    .map(reply -> blogMapper.toPostCommentResponse(reply, profileMap))
                    .collect(Collectors.toList()));
        } else {
            response.setReplies(new ArrayList<>());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PostCommentResponse> getAllComments(PaginationRequest paginationRequest) {
        log.info("Fetching all comments with pagination: {}", paginationRequest);

        Pageable pageable = createPageable(paginationRequest);
        Page<PostComment> commentsPage = postCommentRepository.findAll(pageable);
        List<PostComment> comments = commentsPage.getContent();
        
        Map<String, UserProfileResponse> profileMap = fetchProfilesForComments(comments);

        return PaginatedResponse.<PostCommentResponse>builder()
                .content(comments.stream()
                        .map(comment -> blogMapper.toPostCommentResponse(comment, profileMap))
                        .collect(Collectors.toList()))
                .page(commentsPage.getNumber())
                .size(commentsPage.getSize())
                .totalElements(commentsPage.getTotalElements())
                .totalPages(commentsPage.getTotalPages())
                .first(commentsPage.isFirst())
                .last(commentsPage.isLast())
                .empty(commentsPage.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostCommentResponse> getPostComments(String postId) {
        log.info("Fetching all comments for post: {}", postId);

        // Validate blog post exists
        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        List<PostComment> comments = postCommentRepository.findByPostId(postId);
        Map<String, UserProfileResponse> profileMap = fetchProfilesForComments(comments);
        return buildCommentHierarchy(comments, profileMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostCommentResponse> getApprovedPostComments(String postId) {
        log.info("Fetching approved comments for post: {}", postId);

        // Validate blog post exists
        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        List<PostComment> comments = postCommentRepository.findByPostId(postId)
                .stream()
                .filter(comment -> "APPROVED".equals(comment.getStatus()))
                .collect(Collectors.toList());

        Map<String, UserProfileResponse> profileMap = fetchProfilesForComments(comments);
        return buildCommentHierarchy(comments, profileMap);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PostCommentResponse> getPendingComments(PaginationRequest paginationRequest) {
        log.info("Fetching pending comments with pagination: {}", paginationRequest);

        Pageable pageable = createPageable(paginationRequest);
        Page<PostComment> comments = postCommentRepository.findByStatus("PENDING", pageable);
        List<PostComment> content = comments.getContent();
        
        Map<String, UserProfileResponse> profileMap = fetchProfilesForComments(content);
        
        List<PostCommentResponse> responses = content.stream()
                .map(comment -> blogMapper.toPostCommentResponse(comment, profileMap))
                .collect(Collectors.toList());

        return PaginatedResponse.<PostCommentResponse>builder()
                .content(responses)
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .first(comments.isFirst())
                .last(comments.isLast())
                .empty(comments.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PostCommentResponse> getUserComments(String userId, PaginationRequest paginationRequest) {
        log.info("Fetching comments for user: {}", userId);

        Pageable pageable = createPageable(paginationRequest);
        Page<PostComment> comments = postCommentRepository.findByUserId(userId, pageable);
        List<PostComment> content = comments.getContent();
        
        Map<String, UserProfileResponse> profileMap = fetchProfilesForComments(content);
        
        List<PostCommentResponse> responses = content.stream()
                .map(comment -> blogMapper.toPostCommentResponse(comment, profileMap))
                .collect(Collectors.toList());

        return PaginatedResponse.<PostCommentResponse>builder()
                .content(responses)
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .first(comments.isFirst())
                .last(comments.isLast())
                .empty(comments.isEmpty())
                .build();
    }

    @Override
    public PostCommentResponse updateComment(String commentId, UpdateCommentRequest request, String userId) {
        log.info("Updating comment with ID: {} by user: {}", commentId, userId);

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        // Check if user owns the comment
        if (!comment.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only update your own comments");
        }

        // Check if comment is still editable (not rejected)
        if ("REJECTED".equals(comment.getStatus())) {
            throw new BadRequestException("Cannot update rejected comments");
        }

        try {
            comment.setContent(request.getContent());

            // Reset status to pending if it was approved and now being modified
            if ("APPROVED".equals(comment.getStatus())) {
                comment.setStatus("PENDING");
            }

            // Sync author metadata
            UserProfileResponse profile = null;
            try {
                profile = authServiceClient.getUserProfile(userId);
                if (profile != null) {
                    comment.setAuthorEmail(profile.getEmail());
                    comment.setAuthorFirstName(profile.getFirstName());
                    comment.setAuthorLastName(profile.getLastName());
                    comment.setAuthorAvatarUrl(profile.getAvatarUrl());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user profile for comment updater: {}", userId);
            }

            PostComment updatedComment = postCommentRepository.save(comment);
            log.info("Comment updated successfully with ID: {}", updatedComment.getId());

            return blogMapper.toPostCommentResponse(updatedComment, profile);
        } catch (Exception e) {
            log.error("Error updating comment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update comment: " + e.getMessage());
        }
    }

    @Override
    public PostCommentResponse approveComment(String commentId) {
        log.info("Approving comment with ID: {}", commentId);

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        if ("APPROVED".equals(comment.getStatus())) {
            throw new BadRequestException("Comment is already approved");
        }

        try {
            comment.setStatus("APPROVED");
            PostComment approvedComment = postCommentRepository.save(comment);
            log.info("Comment approved successfully with ID: {}", approvedComment.getId());

            // Enrich with author profile
            UserProfileResponse profile = null;
            try {
                profile = authServiceClient.getUserProfile(approvedComment.getUserId());
            } catch (Exception e) {
                log.warn("Failed to fetch user profile for comment author: {}", approvedComment.getUserId());
            }

            return blogMapper.toPostCommentResponse(approvedComment, profile);
        } catch (Exception e) {
            log.error("Error approving comment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to approve comment: " + e.getMessage());
        }
    }

    @Override
    public PostCommentResponse rejectComment(String commentId) {
        log.info("Rejecting comment with ID: {}", commentId);

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        if ("REJECTED".equals(comment.getStatus())) {
            throw new BadRequestException("Comment is already rejected");
        }

        try {
            comment.setStatus("REJECTED");
            PostComment rejectedComment = postCommentRepository.save(comment);
            log.info("Comment rejected successfully with ID: {}", rejectedComment.getId());

            return blogMapper.toPostCommentResponse(rejectedComment);
        } catch (Exception e) {
            log.error("Error rejecting comment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to reject comment: " + e.getMessage());
        }
    }

    @Override
    public void deleteComment(String commentId, String userId, boolean isAdmin) {
        log.info("Deleting comment with ID: {} by user: {} (admin: {})", commentId, userId, isAdmin);

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        // Check permissions
        if (!isAdmin && !comment.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own comments");
        }

        try {
            // Check if comment has replies
            List<PostComment> replies = postCommentRepository.findRepliesByParentId(commentId);
            if (!replies.isEmpty()) {
                throw new BadRequestException("Cannot delete comment with replies. Please delete replies first.");
            }

            postCommentRepository.delete(comment);
            log.info("Comment deleted successfully with ID: {}", commentId);
        } catch (Exception e) {
            log.error("Error deleting comment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete comment: " + e.getMessage());
        }
    }

    private List<PostCommentResponse> buildCommentHierarchy(List<PostComment> comments, Map<String, UserProfileResponse> profileMap) {
        Map<String, PostCommentResponse> commentMap = new HashMap<>();
        List<PostCommentResponse> rootComments = new ArrayList<>();

        // First pass: create all comment responses
        for (PostComment comment : comments) {
            PostCommentResponse response = blogMapper.toPostCommentResponse(comment, profileMap);
            response.setReplies(new ArrayList<>());
            commentMap.put(comment.getId(), response);
        }

        // Second pass: organize hierarchy
        for (PostComment comment : comments) {
            PostCommentResponse response = commentMap.get(comment.getId());
            if (comment.getParentCommentId() == null) {
                rootComments.add(response);
            } else {
                PostCommentResponse parent = commentMap.get(comment.getParentCommentId());
                if (parent != null) {
                    parent.getReplies().add(response);
                }
            }
        }

        return rootComments;
    }

    private List<PostCommentResponse> buildCommentHierarchy(List<PostComment> comments) {
        return buildCommentHierarchy(comments, new HashMap<>());
    }

    private Pageable createPageable(PaginationRequest request) {
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    private Map<String, UserProfileResponse> fetchProfilesForComments(List<PostComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return new HashMap<>();
        }

        List<String> userIds = comments.stream()
                .map(PostComment::getUserId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            return authServiceClient.getUserProfiles(userIds);
        } catch (Exception e) {
            log.error("Failed to batch fetch user profiles for comments: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}