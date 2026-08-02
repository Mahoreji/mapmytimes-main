package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.PaginationRequest;
import in.mapmytour.blog.dto.request.blogpost.*;
import in.mapmytour.blog.dto.response.blogpost.BlogPostResponse;
import in.mapmytour.blog.dto.response.blogpost.BlogPostSummaryResponse;
import in.mapmytour.blog.dto.response.PaginatedResponse;
import in.mapmytour.blog.dto.response.postlike.PostLikeResponse;
import in.mapmytour.blog.exception.ValidationException;
import in.mapmytour.blog.service.BlogPostService;
import in.mapmytour.blog.service.UserContextService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/blog/posts")
@RequiredArgsConstructor
@Slf4j
public class BlogPostController {

    private final BlogPostService blogPostService;
    private final UserContextService userContextService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(path = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<BlogPostResponse>> createBlogPost(
            @RequestPart("post") String post,
            @RequestParam(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @RequestParam(value = "mediaCaptions", required = false) List<String> mediaCaptions,
            @RequestParam(value = "mediaDescriptions", required = false) List<String> mediaDescriptions,
            @RequestParam(value = "mediaSubtitles", required = false) List<String> mediaSubtitles,
            @RequestParam(value = "groupedMediaFiles", required = false) List<MultipartFile> groupedMediaFiles,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);

        if(userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.<BlogPostResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.UNAUTHORIZED.value())
                            .message("User is not authenticated")
                            .build());
        }

        CreateBlogPostRequest request = parsePostPart(post, CreateBlogPostRequest.class);
        Set<ConstraintViolation<CreateBlogPostRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream().map(ConstraintViolation::getMessage).distinct().reduce((a, b) -> a + "; " + b).orElse("Validation failed");
            throw new ValidationException(message);
        }

        request.setUserId(userId);
        request.setMediaFiles(mediaFiles);
        request.setMediaCaptions(mediaCaptions);
        request.setMediaDescriptions(mediaDescriptions);
        request.setMediaSubtitles(mediaSubtitles);
        request.setGroupedMediaFiles(groupedMediaFiles);
        
        log.info("CreateBlogPost request - mediaGroups: {}, groupedMediaFiles: {}, mediaFiles: {}", 
                request.getMediaGroups() != null ? request.getMediaGroups().size() : 0,
                groupedMediaFiles != null ? groupedMediaFiles.size() : 0,
                mediaFiles != null ? mediaFiles.size() : 0);
        if (request.getMediaGroups() != null && !request.getMediaGroups().isEmpty()) {
            log.info("MediaGroups details: {}", request.getMediaGroups());
        }

        BlogPostResponse response = blogPostService.createBlogPost(request);
        
        // Check if media is included in response
        boolean hasMedia = response.getMedia() != null && !response.getMedia().isEmpty();
        String message = hasMedia 
                ? "Blog post created successfully with media"
                : "Blog post created successfully";

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<BlogPostResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message(message)
                        .data(response)
                        .build());
    }

    @GetMapping("/{postId:[a-f0-9\\-]{36}}")
    public ResponseEntity<APIResponse<BlogPostResponse>> getBlogPost(@PathVariable String postId) {
        BlogPostResponse response = blogPostService.getBlogPost(postId);

        return ResponseEntity.ok(APIResponse.<BlogPostResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog post retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<APIResponse<BlogPostResponse>> getBlogPostBySlug(@PathVariable String slug) {
        BlogPostResponse response = blogPostService.getBlogPostBySlug(slug);

        return ResponseEntity.ok(APIResponse.<BlogPostResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog post retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> getAllBlogPosts(
            @Valid @ModelAttribute PaginationRequest paginationRequest) {

        PaginatedResponse<BlogPostSummaryResponse> response = blogPostService.getAllBlogPosts(paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog posts retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> searchBlogPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String sectionSlug,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) Boolean isTrending,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        BlogPostSearchRequest request = BlogPostSearchRequest.builder()
                .keyword(keyword)
                .categories(category != null ? List.of(category) : null)
                .tags(tag != null ? List.of(tag) : null)
                .sectionSlug(sectionSlug)
                .status(status)
                .postType(postType)
                .isFeatured(isFeatured)
                .isTrending(isTrending)
                .language(language)
                .userId(userId)
                .page(page != null ? page : 0)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PaginatedResponse<BlogPostSummaryResponse> response = blogPostService.searchBlogPosts(request);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog posts search completed successfully")
                .data(response)
                .build());
    }

    @PostMapping("/search")
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> searchBlogPostsPost(
            @Valid @RequestBody BlogPostSearchRequest request) {

        PaginatedResponse<BlogPostSummaryResponse> response = blogPostService.searchBlogPosts(request);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog posts search completed successfully")
                .data(response)
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> getUserBlogPosts(
            @PathVariable String userId,
            @Valid @ModelAttribute PaginationRequest paginationRequest) {

        PaginatedResponse<BlogPostSummaryResponse> response = blogPostService.getUserBlogPosts(userId, paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("User blog posts retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/my-posts")
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> getMyBlogPosts(
            @Valid @ModelAttribute PaginationRequest paginationRequest,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        PaginatedResponse<BlogPostSummaryResponse> response = blogPostService.getUserBlogPosts(userId, paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("My blog posts retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/status")
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> getAllByStatus(
            @RequestParam(required = false) String status,
            @Valid @ModelAttribute PaginationRequest paginationRequest,
            HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        boolean isAdmin = userContextService.isCurrentUserAdmin(httpRequest);
        PaginatedResponse<BlogPostSummaryResponse> response = blogPostService.getAllBlogPostsByStatus(status, userId, isAdmin, paginationRequest);
        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog posts retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<BlogPostResponse>> updateBlogPost(
            @PathVariable String postId,
            @RequestPart("post") String post,
            @RequestParam(value = "newMediaFiles", required = false) List<MultipartFile> newMediaFiles,
            @RequestParam(value = "newMediaCaptions", required = false) List<String> newMediaCaptions,
            @RequestParam(value = "newMediaDescriptions", required = false) List<String> newMediaDescriptions,
            @RequestParam(value = "newMediaSubtitles", required = false) List<String> newMediaSubtitles,
            @RequestParam(value = "newGroupedMediaFiles", required = false) List<MultipartFile> newGroupedMediaFiles,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        boolean isAdmin = userContextService.isCurrentUserAdmin(httpRequest);
        UpdateBlogPostRequest request = parsePostPart(post, UpdateBlogPostRequest.class);
        request.setNewMediaFiles(newMediaFiles);
        request.setNewMediaCaptions(newMediaCaptions);
        request.setNewMediaDescriptions(newMediaDescriptions);
        request.setNewMediaSubtitles(newMediaSubtitles);
        request.setNewGroupedMediaFiles(newGroupedMediaFiles);
        
        log.info("UpdateBlogPost request - newMediaGroups: {}, newGroupedMediaFiles: {}, newMediaFiles: {}", 
                request.getNewMediaGroups() != null ? request.getNewMediaGroups().size() : 0,
                newGroupedMediaFiles != null ? newGroupedMediaFiles.size() : 0,
                newMediaFiles != null ? newMediaFiles.size() : 0);
        if (request.getNewMediaGroups() != null && !request.getNewMediaGroups().isEmpty()) {
            log.info("NewMediaGroups details: {}", request.getNewMediaGroups());
        }

        BlogPostResponse response = blogPostService.updateBlogPost(postId, request, userId, isAdmin);

        return ResponseEntity.ok(APIResponse.<BlogPostResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog post updated successfully")
                .data(response)
                .build());
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse<BlogPostResponse>> updateBlogPostJson(
            @PathVariable String postId,
            @RequestBody UpdateBlogPostRequest request,
            HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        boolean isAdmin = userContextService.isCurrentUserAdmin(httpRequest);
        BlogPostResponse response = blogPostService.updateBlogPost(postId, request, userId, isAdmin);
        return ResponseEntity.ok(APIResponse.<BlogPostResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog post updated successfully")
                .data(response)
                .build());
    }

    private <T> T parsePostPart(String post, Class<T> type) {
        String raw = post == null ? "" : post.trim();
        if (raw.isEmpty()) {
            throw new ValidationException("Missing 'post' payload");
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node != null && node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            return objectMapper.treeToValue(node, type);
        } catch (Exception first) {
            try {
                String unescaped = objectMapper.readValue(raw, String.class);
                JsonNode node = objectMapper.readTree(unescaped);
                if (node != null && node.isTextual()) {
                    node = objectMapper.readTree(node.asText());
                }
                return objectMapper.treeToValue(node, type);
            } catch (Exception second) {
                throw new ValidationException("Invalid JSON in 'post' part");
            }
        }
    }

    @PostMapping("/{postId}/publish")
    public ResponseEntity<APIResponse<BlogPostResponse>> publishBlogPost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        boolean isAdmin = userContextService.isCurrentUserAdmin(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.<BlogPostResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.UNAUTHORIZED.value())
                            .message("User is not authenticated")
                            .build());
        }
        BlogPostResponse response = blogPostService.publishBlogPost(postId, userId, isAdmin);

        return ResponseEntity.ok(APIResponse.<BlogPostResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog post published successfully")
                .data(response)
                .build());
    }

    @PostMapping("/{postId}/unpublish")
    public ResponseEntity<APIResponse<BlogPostResponse>> unpublishBlogPost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        boolean isAdmin = userContextService.isCurrentUserAdmin(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.<BlogPostResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.UNAUTHORIZED.value())
                            .message("User is not authenticated")
                            .build());
        }
        BlogPostResponse response = blogPostService.unpublishBlogPost(postId, userId, isAdmin);

        return ResponseEntity.ok(APIResponse.<BlogPostResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog post unpublished successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<APIResponse<Void>> deleteBlogPost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        boolean isAdmin = userContextService.isCurrentUserAdmin(httpRequest);
        blogPostService.deleteBlogPost(postId, userId, isAdmin);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog post deleted successfully")
                .build());
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<APIResponse<Void>> likePost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        blogPostService.likePost(postId, userId);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Post liked successfully")
                .build());
    }

    @PutMapping("/{postId}/view")
    public ResponseEntity<APIResponse<Void>> incrementViewCount(@PathVariable String postId) {
        blogPostService.incrementViewCount(postId);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("View count incremented")
                .build());
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<APIResponse<Void>> unlikePost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        blogPostService.unlikePost(postId, userId);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Post unliked successfully")
                .build());
    }

    @GetMapping("/{postId}/likes")
    public ResponseEntity<APIResponse<List<PostLikeResponse>>> getPostLikes(@PathVariable String postId) {
        List<PostLikeResponse> response = blogPostService.getPostLikes(postId);

        return ResponseEntity.ok(APIResponse.<List<PostLikeResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Post likes retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/my-likes")
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> getMyLikedPosts(
            @Valid @ModelAttribute PaginationRequest paginationRequest,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        PaginatedResponse<BlogPostSummaryResponse> response = blogPostService.getMyLikedPosts(userId, paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("My liked posts retrieved successfully")
                .data(response)
                .build());
    }
}
