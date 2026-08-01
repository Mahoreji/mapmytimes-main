package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.postlike.LikePostRequest;
import in.mapmytour.blog.dto.response.postlike.PostLikeResponse;
import in.mapmytour.blog.entity.BlogPost;
import in.mapmytour.blog.entity.PostLike;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.exception.DuplicateResourceException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.repository.BlogPostRepository;
import in.mapmytour.blog.repository.PostLikeRepository;
import in.mapmytour.blog.service.PostLikeService;
import in.mapmytour.blog.utils.BlogMapper;
import in.mapmytour.blog.client.AuthServiceClient;
import in.mapmytour.blog.dto.external.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogMapper blogMapper;
    private final AuthServiceClient authServiceClient;

    @Override
    public PostLikeResponse likePost(LikePostRequest request) {
        log.info("User {} liking post: {}", request.getUserId(), request.getPostId());

        // Validate blog post exists
        BlogPost blogPost = blogPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + request.getPostId()));

        // Check if likes are allowed on this post
        if (!blogPost.getAllowLikes()) {
            throw new BadRequestException("Likes are not allowed on this blog post");
        }

        // Check if user has already liked this post
        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(request.getPostId(), request.getUserId());
        if (existingLike.isPresent()) {
            throw new DuplicateResourceException("You have already liked this post");
        }

        try {
            // Fetch author metadata from auth-service if userId is present
            String authorEmail = null;
            String authorFirstName = null;
            String authorLastName = null;
            String authorAvatarUrl = null;
            UserProfileResponse profile = null;

            if (org.springframework.util.StringUtils.hasText(request.getUserId())) {
                log.debug("Fetching author profile for like creator: {}", request.getUserId());
                try {
                    profile = authServiceClient.getUserProfile(request.getUserId());
                    if (profile != null) {
                        authorEmail = profile.getEmail();
                        authorFirstName = profile.getFirstName();
                        authorLastName = profile.getLastName();
                        authorAvatarUrl = profile.getAvatarUrl();
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch user profile for like creator: {}", request.getUserId());
                }
            }

            PostLike like = PostLike.builder()
                    .post(blogPost)
                    .userId(request.getUserId())
                    .authorEmail(authorEmail)
                    .authorFirstName(authorFirstName)
                    .authorLastName(authorLastName)
                    .authorAvatarUrl(authorAvatarUrl)
                    .build();

            PostLike savedLike = postLikeRepository.save(like);
            log.info("Post liked successfully with ID: {}", savedLike.getId());

            // Send notification to post author
            if (!request.getUserId().equals(blogPost.getUserId())) {
                String message = (authorFirstName != null ? authorFirstName : "Someone") + " liked your post";
                authServiceClient.sendNotification(
                        blogPost.getUserId(),
                        request.getUserId(),
                        "SOCIAL_LIKE",
                        message,
                        blogPost.getId()
                );
            }

            return blogMapper.toPostLikeResponse(savedLike, profile);
        } catch (Exception e) {
            log.error("Error liking post: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to like post: " + e.getMessage());
        }
    }

    @Override
    public void unlikePost(String postId, String userId) {
        log.info("User {} unliking post: {}", userId, postId);

        // Validate blog post exists
        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        // Check if user has liked this post
        PostLike like = postLikeRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You have not liked this post"));

        try {
            postLikeRepository.delete(like);
            log.info("Post unliked successfully for user: {} and post: {}", userId, postId);
        } catch (Exception e) {
            log.error("Error unliking post: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to unlike post: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostLikeResponse> getPostLikes(String postId) {
        log.info("Fetching likes for post: {}", postId);

        // Validate blog post exists
        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        List<PostLike> likes = postLikeRepository.findByPostId(postId);
        Map<String, UserProfileResponse> profileMap = fetchProfilesForLikes(likes);
        return likes.stream()
                .map(like -> blogMapper.toPostLikeResponse(like, profileMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getPostLikeCount(String postId) {
        log.info("Fetching like count for post: {}", postId);

        // Validate blog post exists
        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        return (int) postLikeRepository.countByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isPostLikedByUser(String postId, String userId) {
        log.info("Checking if user {} has liked post: {}", userId, postId);

        // Validate blog post exists
        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostLikeResponse> getMyLikedPosts(String userId) {
        log.info("Fetching liked posts for user: {}", userId);

        List<PostLike> likes = postLikeRepository.findByUserId(userId);
        Map<String, UserProfileResponse> profileMap = fetchProfilesForLikes(likes);
        return likes.stream()
                .map(like -> blogMapper.toPostLikeResponse(like, profileMap))
                .collect(Collectors.toList());
    }

    private Map<String, UserProfileResponse> fetchProfilesForLikes(List<PostLike> likes) {
        if (likes == null || likes.isEmpty()) {
            return new HashMap<>();
        }

        List<String> userIds = likes.stream()
                .map(PostLike::getUserId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            return authServiceClient.getUserProfiles(userIds);
        } catch (Exception e) {
            log.error("Failed to batch fetch user profiles for likes: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}