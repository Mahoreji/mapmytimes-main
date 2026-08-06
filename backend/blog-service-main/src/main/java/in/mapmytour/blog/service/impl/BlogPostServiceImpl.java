package in.mapmytour.blog.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import in.mapmytour.blog.dto.request.PaginationRequest;
import in.mapmytour.blog.dto.request.blogpost.BlogPostSearchRequest;
import in.mapmytour.blog.dto.request.blogpost.CreateBlogPostRequest;
import in.mapmytour.blog.dto.request.blogpost.MediaGroupRequest;
import in.mapmytour.blog.dto.request.blogpost.UpdateBlogPostRequest;
import in.mapmytour.blog.dto.response.PaginatedResponse;
import in.mapmytour.blog.dto.response.blogpost.BlogPostResponse;
import in.mapmytour.blog.dto.response.blogpost.BlogPostSummaryResponse;
import in.mapmytour.blog.dto.response.postlike.PostLikeResponse;
import in.mapmytour.blog.entity.BlogPost;
import in.mapmytour.blog.entity.PostComment;
import in.mapmytour.blog.entity.PostLike;
import in.mapmytour.blog.entity.PostMedia;
import in.mapmytour.blog.exception.ForbiddenException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.exception.DuplicateResourceException;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.helper.S3Helper;
import in.mapmytour.blog.repository.BlogPostRepository;
import in.mapmytour.blog.repository.PostMediaRepository;
import in.mapmytour.blog.service.BlogPostService;
import in.mapmytour.blog.service.PostLikeService;
import in.mapmytour.blog.service.AsyncMediaUploadService;
import in.mapmytour.blog.utils.BlogMapper;
import in.mapmytour.blog.utils.SlugUtil;
import in.mapmytour.blog.client.AuthServiceClient;
import in.mapmytour.blog.dto.external.UserProfileResponse;
import in.mapmytour.blog.event.BlogEventProducer;
import org.slf4j.MDC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final PostMediaRepository postMediaRepository;
    private final S3Helper s3Helper;
    private final BlogMapper blogMapper;
    private final PostLikeService postLikeService;
    private final BlogEventProducer blogEventProducer;
    private final AsyncMediaUploadService asyncMediaUploadService;
    private final AuthServiceClient authServiceClient;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public BlogPostResponse createBlogPost(CreateBlogPostRequest request) {
        log.info("Creating blog post with title: {}", request.getTitle());

        // Check if slug already exists
        if (blogPostRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Blog post with slug '" + request.getSlug() + "' already exists");
        }

        try {
            // Fetch author metadata from auth-service if userId is present
            String authorEmail = request.getAuthorEmail();
            String authorFirstName = request.getAuthorFirstName();
            String authorLastName = request.getAuthorLastName();
            String authorAvatarUrl = request.getAuthorAvatarUrl();

            UserProfileResponse profile = null;
            if (StringUtils.hasText(request.getUserId())) {
                log.debug("Fetching author profile for userId: {}", request.getUserId());
                profile = authServiceClient.getUserProfile(request.getUserId());
                if (profile != null) {
                    authorEmail = profile.getEmail();
                    authorFirstName = profile.getFirstName();
                    authorLastName = profile.getLastName();
                    authorAvatarUrl = profile.getAvatarUrl();
                    log.debug("Found profile: {} {}, email: {}", authorFirstName, authorLastName, authorEmail);
                } else {
                    log.warn("Could not find profile in auth-service for userId: {}", request.getUserId());
                }
            }

            String slug = request.getSlug();
            if (!StringUtils.hasText(slug)) {
                slug = SlugUtil.generateSlug(request.getTitle());
            }
            // Ensure unique slug
            slug = SlugUtil.ensureUniqueSlug(slug, blogPostRepository::existsBySlug);

            // Create blog post entity — readingTime auto-computed (P0-1), ignore any client-sent value
            final Integer autoReadingTime = computeReadingTime(request.getContent());
            BlogPost blogPost = BlogPost.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .slug(slug)
                    .excerpt(request.getExcerpt())
                    .readingTime(autoReadingTime)
                    .featuredImage(request.getFeaturedImage())
                    .primaryVideoUrl(request.getPrimaryVideoUrl())
                    .contentBlocks(request.getContentBlocks())
                    .tableOfContents(request.getTableOfContents())
                    .travelMeta(request.getTravelMeta())
                    .seo(request.getSeo())
                    .visibility(org.springframework.util.StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "PUBLIC")
                    .language(org.springframework.util.StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : "en")
                    .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                    .isTrending(request.getIsTrending() != null ? request.getIsTrending() : false)
                    .scheduledAt(request.getScheduledAt())
                    .userId(request.getUserId())
                    .categories(request.getCategories() != null ? request.getCategories() : new ArrayList<>())
                    .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                    .sectionSlug(request.getSectionSlug())
                    .allowLikes(request.getAllowLikes())
                    .status("DRAFT")
                    .postType(org.springframework.util.StringUtils.hasText(request.getPostType()) ? request.getPostType() : "BLOG")
                    .authorEmail(authorEmail)
                    .authorFirstName(authorFirstName)
                    .authorLastName(authorLastName)
                    .authorAvatarUrl(authorAvatarUrl)
                    .build();

            // Save blog post
            BlogPost savedPost = blogPostRepository.saveAndFlush(blogPost);
            entityManager.flush();
            savedPost = blogPostRepository.findByIdWithMedia(savedPost.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Blog post not found after creation"));

            // Upload media files if provided.
            // Check if using new grouped structure or legacy flat structure.
            log.info("Media upload check - mediaGroups: {}, groupedMediaFiles: {}, mediaFiles: {}",
                    request.getMediaGroups() != null ? request.getMediaGroups().size() : 0,
                    request.getGroupedMediaFiles() != null ? request.getGroupedMediaFiles().size() : 0,
                    request.getMediaFiles() != null ? request.getMediaFiles().size() : 0);

            // Copy MultipartFile content to byte arrays before async processing
            // (MultipartFile streams cannot be read after the HTTP request completes).
            CompletableFuture<List<PostMedia>> uploadFuture = null;

            if (request.getMediaGroups() != null && !request.getMediaGroups().isEmpty()
                    && request.getGroupedMediaFiles() != null && !request.getGroupedMediaFiles().isEmpty()) {
                // New grouped structure: multiple images per subtitle.
                log.info("Using grouped media structure with {} groups and {} files - uploading",
                        request.getMediaGroups().size(), request.getGroupedMediaFiles().size());
                List<MultipartFile> copiedFiles = copyMultipartFiles(request.getGroupedMediaFiles());
                uploadFuture = asyncMediaUploadService.uploadGroupedMediaFilesAsync(savedPost.getId(),
                        request.getMediaGroups(), copiedFiles, request.getUserId());
            } else if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
                // Legacy flat structure: one subtitle per image.
                log.info("Using legacy flat media structure with {} files - uploading", request.getMediaFiles().size());
                List<MultipartFile> copiedFiles = copyMultipartFiles(request.getMediaFiles());
                uploadFuture = asyncMediaUploadService.uploadMediaFilesAsync(savedPost.getId(), copiedFiles,
                        request.getMediaCaptions(),
                        request.getMediaDescriptions(),
                        request.getMediaSubtitles(),
                        request.getUserId());
            } else {
                log.warn("No media files provided for blog post creation");
            }

            // Wait for media uploads to complete (with timeout to avoid Cloudflare 60s
            // limit).
            // This ensures image links are included in the response.
            // Wait for media uploads to complete (parallelized inside the async service).
            if (uploadFuture != null) {
                try {
                    log.info("Waiting for parallel media uploads to complete (max 120 seconds)...");
                    List<PostMedia> uploadedMedia = uploadFuture.get(120, TimeUnit.SECONDS);
                    log.info("Media uploads completed successfully");

                    if (uploadedMedia != null && !uploadedMedia.isEmpty()) {
                        // The async service now saves media itself, but we should attach them to the
                        // current blogPost instance if possible for immediate response feedback.
                        // We filter for media that wasn't already attached.
                        if (savedPost.getMedia() == null) {
                            savedPost.setMedia(new ArrayList<>());
                        }
                        
                        Set<String> existingUrls = savedPost.getMedia().stream()
                                .map(PostMedia::getMediaUrl)
                                .collect(Collectors.toSet());
                        
                        for (PostMedia media : uploadedMedia) {
                            if (!existingUrls.contains(media.getMediaUrl())) {
                                media.setPost(savedPost);
                                savedPost.getMedia().add(media);
                            }
                        }
                        log.info("Attached {} media items to blog post for response", uploadedMedia.size());
                    }

                } catch (TimeoutException e) {
                    log.warn("Media upload timeout after 120 seconds. Persistence will be handled in the background.");
                } catch (Exception e) {
                    log.error("Error waiting for media uploads: {}", e.getMessage(), e);
                }
            }

            savedPost = applyUploadedMediaUrls(savedPost);

            // Publish blog post created event to Kafka
            try {
                String correlationId = MDC.get("correlationId");
                if (correlationId == null) {
                    correlationId = java.util.UUID.randomUUID().toString();
                }
                blogEventProducer.publishBlogPostCreated(
                        savedPost.getId(),
                        savedPost.getTitle(),
                        savedPost.getUserId(),
                        correlationId);
            } catch (Exception e) {
                log.warn("Failed to publish blog post created event for post {}: {}", savedPost.getId(),
                        e.getMessage());
            }

            log.info("Blog post created successfully with ID: {}", savedPost.getId());
            
            // Initialize lazy collections to avoid LazyInitializationException during serialization
            initializeLazyCollections(savedPost);
            
            return blogMapper.toBlogPostResponse(savedPost, profile);

        } catch (Exception e) {
            log.error("Error creating blog post: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create blog post: " + e.getMessage());
        }
    }

    @Override
    public BlogPostResponse getBlogPost(String postId) {
        log.info("Fetching blog post with ID: {}", postId);

        // Increment view count asynchronously or within the same transaction
        incrementViewCount(postId);

        BlogPost blogPost = blogPostRepository.findByIdWithMedia(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + postId));

        // Initialize lazy collections
        initializeLazyCollections(blogPost);

        // Fetch fresh profile from auth-service for post author
        UserProfileResponse profile = null;
        if (StringUtils.hasText(blogPost.getUserId())) {
            profile = authServiceClient.getUserProfile(blogPost.getUserId());
        }

        // Fetch profiles for comment authors
        List<PostComment> approvedComments = blogPost.getComments().stream()
                .filter(PostComment::isApproved)
                .collect(Collectors.toList());
        Map<String, UserProfileResponse> commentProfileMap = fetchProfilesForComments(approvedComments);

        BlogPostResponse response = blogMapper.toBlogPostResponse(blogPost, profile, commentProfileMap);
        
        // Populate related posts
        response.setRelatedPosts(fetchRelatedPosts(blogPost, 10));
        
        return response;
    }

    @Override
    public BlogPostResponse getBlogPostBySlug(String slug) {
        log.info("Fetching blog post with slug: {}", slug);

        BlogPost blogPost = blogPostRepository.findBySlugWithMedia(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with slug: " + slug));

        // Increment view count
        incrementViewCount(blogPost.getId());

        // Initialize lazy collections
        initializeLazyCollections(blogPost);

        // Fetch fresh profile from auth-service for post author
        UserProfileResponse profile = null;
        if (StringUtils.hasText(blogPost.getUserId())) {
            profile = authServiceClient.getUserProfile(blogPost.getUserId());
        }

        // Fetch profiles for comment authors
        List<PostComment> approvedComments = blogPost.getComments().stream()
                .filter(PostComment::isApproved)
                .collect(Collectors.toList());
        Map<String, UserProfileResponse> commentProfileMap = fetchProfilesForComments(approvedComments);

        BlogPostResponse response = blogMapper.toBlogPostResponse(blogPost, profile, commentProfileMap);
        
        // Populate related posts
        response.setRelatedPosts(fetchRelatedPosts(blogPost, 10));
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BlogPostSummaryResponse> getAllBlogPosts(PaginationRequest paginationRequest) {
        log.info("Fetching all blog posts with pagination: {}", paginationRequest);

        String language = StringUtils.hasText(paginationRequest.getLanguage()) ? paginationRequest.getLanguage() : null;

        // If size is null, 0, or >= 10000, return all results without pagination
        if (paginationRequest.getSize() == null || paginationRequest.getSize() <= 0
                || paginationRequest.getSize() >= 10000) {
            log.info("Fetching all blog posts without pagination limit");
            List<BlogPost> allPosts = language != null
                    ? blogPostRepository.findByStatusAndLanguage("PUBLISHED", language)
                    : blogPostRepository.findByStatus("PUBLISHED");

            // Apply sorting manually
            String sortBy = paginationRequest.getSortBy() != null ? paginationRequest.getSortBy() : "createdAt";
            String sortDirection = paginationRequest.getSortDirection() != null ? paginationRequest.getSortDirection()
                    : "DESC";

            allPosts.sort((a, b) -> {
                int comparison = switch (sortBy) {
                    case "updatedAt" -> a.getUpdatedAt().compareTo(b.getUpdatedAt());
                    case "title" -> a.getTitle().compareToIgnoreCase(b.getTitle());
                    default -> a.getCreatedAt().compareTo(b.getCreatedAt());
                };
                return "DESC".equalsIgnoreCase(sortDirection) ? -comparison : comparison;
            });

            // Initialize lazy collections
            initializeLazyCollections(allPosts);

            Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(allPosts);
            List<BlogPostSummaryResponse> responses = allPosts.stream()
                    .map(post -> blogMapper.toBlogPostSummaryResponse(post, profileMap.get(post.getUserId())))
                    .collect(Collectors.toList());

            return PaginatedResponse.<BlogPostSummaryResponse>builder()
                    .content(responses)
                    .page(0)
                    .size(responses.size())
                    .totalElements((long) responses.size())
                    .totalPages(1)
                    .last(true)
                    .build();
        }

        log.info("Fetching all blog posts with pagination");
        Pageable pageable = createPageable(paginationRequest);
        Page<BlogPost> blogPosts = language != null
                ? blogPostRepository.findByStatusAndLanguage("PUBLISHED", language, pageable)
                : blogPostRepository.findByStatus("PUBLISHED", pageable);

        // Initialize lazy collections
        initializeLazyCollections(blogPosts.getContent());

        Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(blogPosts.getContent());
        Page<BlogPostSummaryResponse> responsePage = blogPosts
                .map(post -> blogMapper.toBlogPostSummaryResponse(post, profileMap.get(post.getUserId())));
        return blogMapper.toPaginatedResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BlogPostSummaryResponse> searchBlogPosts(BlogPostSearchRequest request) {
        log.info("Searching blog posts with criteria: {}", request);

        String language = StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : null;
        Integer size = request.getSize() != null ? request.getSize() : 10;
        Integer page = request.getPage() != null ? request.getPage() : 0;

        // If size is null, 0, or >= 10000, return all results without pagination
        if (size <= 0 || size >= 10000) {
            log.info("Searching blog posts without pagination limit");
            List<BlogPost> allPosts;

            // Handle section-specific search
            if (StringUtils.hasText(request.getSectionSlug())) {
                Pageable unlimitedPageable = PageRequest.of(0, 10000,
                        Sort.by(Sort.Direction
                                .fromString(request.getSortDirection() != null ? request.getSortDirection() : "DESC"),
                                request.getSortBy() != null ? request.getSortBy() : "createdAt"));
                Page<BlogPost> sectionPage = blogPostRepository.findBySectionSlug(request.getSectionSlug(),
                        unlimitedPageable);
                allPosts = sectionPage.getContent();
            }
            // Handle category-specific search
            else if (request.getCategories() != null && !request.getCategories().isEmpty()) {
                // Get all posts for category (we'll need to fetch all and filter)
                Pageable unlimitedPageable = PageRequest.of(0, 10000,
                        Sort.by(Sort.Direction
                                .fromString(request.getSortDirection() != null ? request.getSortDirection() : "DESC"),
                                request.getSortBy() != null ? request.getSortBy() : "createdAt"));
                Page<BlogPost> categoryPage = blogPostRepository.findByCategory(request.getCategories().get(0),
                        unlimitedPageable);
                allPosts = categoryPage.getContent();
                if (StringUtils.hasText(request.getSectionSlug())) {
                    allPosts = allPosts.stream()
                            .filter(p -> request.getSectionSlug().equalsIgnoreCase(p.getSectionSlug()))
                            .collect(Collectors.toList());
                }
            }
            // Handle tag-specific search
            else if (request.getTags() != null && !request.getTags().isEmpty()) {
                Pageable unlimitedPageable = PageRequest.of(0, 10000,
                        Sort.by(Sort.Direction
                                .fromString(request.getSortDirection() != null ? request.getSortDirection() : "DESC"),
                                request.getSortBy() != null ? request.getSortBy() : "createdAt"));
                Page<BlogPost> tagPage = blogPostRepository.findByTag(request.getTags().get(0), unlimitedPageable);
                allPosts = tagPage.getContent();
                if (StringUtils.hasText(request.getSectionSlug())) {
                    allPosts = allPosts.stream()
                            .filter(p -> request.getSectionSlug().equalsIgnoreCase(p.getSectionSlug()))
                            .collect(Collectors.toList());
                }
            }
            // Handle general search - use custom method for full-text search including
            // content
            else {
                Pageable unlimitedPageable = PageRequest.of(0, 10000,
                        Sort.by(Sort.Direction
                                .fromString(request.getSortDirection() != null ? request.getSortDirection() : "DESC"),
                                request.getSortBy() != null ? request.getSortBy() : "createdAt"));
                Page<BlogPost> searchPage = blogPostRepository.searchPostsWithContent(
                        request.getKeyword(),
                        request.getStatus(),
                        request.getUserId(),
                        unlimitedPageable);
                allPosts = searchPage.getContent();
                if (StringUtils.hasText(request.getSectionSlug())) {
                    allPosts = allPosts.stream()
                            .filter(p -> request.getSectionSlug().equalsIgnoreCase(p.getSectionSlug()))
                            .collect(Collectors.toList());
                }
            }

            // Apply language filter in-memory (works for all branches: section/category/tag/search)
            if (language != null) {
                allPosts = allPosts.stream()
                        .filter(p -> language.equalsIgnoreCase(p.getLanguage()))
                        .collect(Collectors.toList());
            }

            // Apply uniform filters: status + postType + userId + isFeatured + isTrending
            allPosts = allPosts.stream()
                    .filter(p -> request.getStatus() == null || request.getStatus().equalsIgnoreCase(p.getStatus()))
                    .filter(p -> request.getPostType() == null || request.getPostType().equalsIgnoreCase(p.getPostType()))
                    .filter(p -> request.getUserId() == null || request.getUserId().equals(p.getUserId()))
                    .filter(p -> request.getIsFeatured() == null || request.getIsFeatured().equals(p.getIsFeatured()))
                    .filter(p -> request.getIsTrending() == null || request.getIsTrending().equals(p.getIsTrending()))
                    .collect(Collectors.toList());

            // Initialize lazy collections
            initializeLazyCollections(allPosts);

            Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(allPosts);
            List<BlogPostSummaryResponse> allResponses = allPosts.stream()
                    .map(post -> blogMapper.toBlogPostSummaryResponse(post, profileMap.get(post.getUserId())))
                    .toList();

            return PaginatedResponse.<BlogPostSummaryResponse>builder()
                    .content(allResponses)
                    .page(0)
                    .size(allResponses.size())
                    .totalElements((long) allResponses.size())
                    .totalPages(1)
                    .last(true)
                    .build();
        }

        Pageable pageable = createPageable(PaginationRequest.builder()
                .page(page)
                .size(size)
                .sortBy(request.getSortBy() != null ? request.getSortBy() : "createdAt")
                .sortDirection(request.getSortDirection() != null ? request.getSortDirection() : "DESC")
                .build());

        Page<BlogPost> blogPosts;

        // Handle section-specific search
        if (StringUtils.hasText(request.getSectionSlug())) {
            blogPosts = blogPostRepository.findBySectionSlug(request.getSectionSlug(), pageable);
        }
        // Handle category-specific search
        else if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            // For now, search by the first category (can be enhanced later for multiple
            // categories)
            blogPosts = blogPostRepository.findByCategory(request.getCategories().get(0), pageable);
            if (StringUtils.hasText(request.getSectionSlug())) {
                List<BlogPost> filtered = blogPosts.getContent().stream()
                        .filter(p -> request.getSectionSlug().equalsIgnoreCase(p.getSectionSlug()))
                        .collect(Collectors.toList());
                blogPosts = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
        }
        // Handle tag-specific search
        else if (request.getTags() != null && !request.getTags().isEmpty()) {
            // For now, search by the first tag (can be enhanced later for multiple tags)
            blogPosts = blogPostRepository.findByTag(request.getTags().get(0), pageable);
            if (StringUtils.hasText(request.getSectionSlug())) {
                List<BlogPost> filtered = blogPosts.getContent().stream()
                        .filter(p -> request.getSectionSlug().equalsIgnoreCase(p.getSectionSlug()))
                        .collect(Collectors.toList());
                blogPosts = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
        }
        // Handle general search - use custom method for full-text search including
        // content
        else {
            blogPosts = blogPostRepository.searchPostsWithContent(
                    request.getKeyword(),
                    request.getStatus(),
                    request.getUserId(),
                    pageable);
            if (StringUtils.hasText(request.getSectionSlug())) {
                List<BlogPost> filtered = blogPosts.getContent().stream()
                        .filter(p -> request.getSectionSlug().equalsIgnoreCase(p.getSectionSlug()))
                        .collect(Collectors.toList());
                blogPosts = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
        }

        // For section/category/tag/search pages with pagination, apply language filter in-memory
        // (keeping it simple; later we could introduce custom @Query methods with AND language)
        if (language != null) {
            List<BlogPost> filtered = blogPosts.getContent().stream()
                    .filter(p -> language.equalsIgnoreCase(p.getLanguage()))
                    .collect(Collectors.toList());
            blogPosts = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }

        // Apply uniform filters: status + postType + userId + isFeatured + isTrending
        {
            List<BlogPost> filtered = blogPosts.getContent().stream()
                    .filter(p -> request.getStatus() == null || request.getStatus().equalsIgnoreCase(p.getStatus()))
                    .filter(p -> request.getPostType() == null || request.getPostType().equalsIgnoreCase(p.getPostType()))
                    .filter(p -> request.getUserId() == null || request.getUserId().equals(p.getUserId()))
                    .filter(p -> request.getIsFeatured() == null || request.getIsFeatured().equals(p.getIsFeatured()))
                    .filter(p -> request.getIsTrending() == null || request.getIsTrending().equals(p.getIsTrending()))
                    .collect(Collectors.toList());
            blogPosts = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }

        // Initialize lazy collections
        initializeLazyCollections(blogPosts.getContent());

        Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(blogPosts.getContent());
        Page<BlogPostSummaryResponse> responsePage = blogPosts
                .map(post -> blogMapper.toBlogPostSummaryResponse(post, profileMap.get(post.getUserId())));
        return blogMapper.toPaginatedResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BlogPostSummaryResponse> getUserBlogPosts(String userId,
            PaginationRequest paginationRequest) {
        log.info("Fetching blog posts for user: {}", userId);

        Pageable pageable = createPageable(paginationRequest);
        Page<BlogPost> blogPosts = blogPostRepository.findByUserId(userId, pageable);

        // Initialize lazy collections
        initializeLazyCollections(blogPosts.getContent());

        Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(blogPosts.getContent());
        Page<BlogPostSummaryResponse> responsePage = blogPosts
                .map(post -> blogMapper.toBlogPostSummaryResponse(post, profileMap.get(post.getUserId())));
        return blogMapper.toPaginatedResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BlogPostSummaryResponse> getPersonalizedFeed(List<String> userIds, List<String> postTypes,
            PaginationRequest paginationRequest) {
        log.info("Fetching personalized feed for {} users with post types: {}", userIds != null ? userIds.size() : 0,
                postTypes);

        String language = StringUtils.hasText(paginationRequest.getLanguage()) ? paginationRequest.getLanguage() : null;
        Pageable pageable = createPageable(paginationRequest);
        Page<BlogPost> blogPosts;

        if (userIds != null && !userIds.isEmpty() && postTypes != null && !postTypes.isEmpty()) {
            blogPosts = language != null
                    ? blogPostRepository.findByUserIdInAndStatusAndPostTypeInAndLanguage(userIds, "PUBLISHED", postTypes, language, pageable)
                    : blogPostRepository.findByUserIdInAndStatusAndPostTypeIn(userIds, "PUBLISHED", postTypes, pageable);
        } else if (postTypes != null && !postTypes.isEmpty()) {
            blogPosts = language != null
                    ? blogPostRepository.findByStatusAndPostTypeInAndLanguage("PUBLISHED", postTypes, language, pageable)
                    : blogPostRepository.findByStatusAndPostTypeIn("PUBLISHED", postTypes, pageable);
        } else if (userIds != null && !userIds.isEmpty()) {
            if (language != null) {
                blogPosts = blogPostRepository.findByUserIdInAndStatusAndPostTypeInAndLanguage(userIds, "PUBLISHED",
                        List.of("BLOG", "SOCIAL", "STORY"), language, pageable);
            } else {
                blogPosts = blogPostRepository.findByUserIdInAndStatusAndPostTypeIn(userIds, "PUBLISHED",
                        List.of("BLOG", "SOCIAL", "STORY"), pageable);
            }
        } else {
            blogPosts = language != null
                    ? blogPostRepository.findByStatusAndLanguage("PUBLISHED", language, pageable)
                    : blogPostRepository.findByStatus("PUBLISHED", pageable);
        }

        // Initialize lazy collections
        initializeLazyCollections(blogPosts.getContent());

        Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(blogPosts.getContent());
        Page<BlogPostSummaryResponse> responsePage = blogPosts
                .map(post -> blogMapper.toBlogPostSummaryResponse(post, profileMap.get(post.getUserId())));
        return blogMapper.toPaginatedResponse(responsePage);
    }

    @Override
    public BlogPostResponse updateBlogPost(String postId, UpdateBlogPostRequest request, String userId,
            boolean isAdmin) {
        log.info("Updating blog post with ID: {} by user: {}", postId, userId);

        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + postId));

        // Check if user owns the post
        if (!blogPost.getUserId().equals(userId) && !isAdmin) {
            throw new ForbiddenException("You can only update your own blog posts");
        }

        try {
            // Extract all current URLs BEFORE applying the new JSON payload
            Set<String> oldUrls = extractAllMediaUrlsFromPost(blogPost);

            // Update fields if provided
            if (StringUtils.hasText(request.getTitle())) {
                blogPost.setTitle(request.getTitle());
            }
            if (StringUtils.hasText(request.getContent())) {
                blogPost.setContent(request.getContent());
            }
            if (StringUtils.hasText(request.getSlug()) && !request.getSlug().equals(blogPost.getSlug())) {
                String slug = SlugUtil.ensureUniqueSlug(request.getSlug(), blogPostRepository::existsBySlug);
                blogPost.setSlug(slug);
            }
            if (StringUtils.hasText(request.getExcerpt())) {
                blogPost.setExcerpt(request.getExcerpt());
            }
            // P0-1: readingTime auto-recomputed whenever content changes (editor manual input IGNORED)
            if (StringUtils.hasText(request.getContent())) {
                final Integer recomputed = computeReadingTime(request.getContent());
                if (recomputed != null) {
                    blogPost.setReadingTime(recomputed);
                }
            }
            if (request.getFeaturedImage() != null) {
                blogPost.setFeaturedImage(request.getFeaturedImage());
            }
            if (request.getPrimaryVideoUrl() != null) {
                blogPost.setPrimaryVideoUrl(request.getPrimaryVideoUrl());
            }
            if (request.getContentBlocks() != null) {
                blogPost.setContentBlocks(request.getContentBlocks());
            }
            if (request.getTableOfContents() != null) {
                blogPost.setTableOfContents(request.getTableOfContents());
            }
            if (request.getTravelMeta() != null) {
                blogPost.setTravelMeta(request.getTravelMeta());
            }
            if (request.getSeo() != null) {
                blogPost.setSeo(request.getSeo());
            }
            if (StringUtils.hasText(request.getVisibility())) {
                blogPost.setVisibility(request.getVisibility());
            }
            if (StringUtils.hasText(request.getLanguage())) {
                blogPost.setLanguage(request.getLanguage());
            }
            if (request.getIsFeatured() != null) {
                blogPost.setIsFeatured(request.getIsFeatured());
            }
            if (request.getIsTrending() != null) {
                blogPost.setIsTrending(request.getIsTrending());
            }
            if (request.getScheduledAt() != null) {
                blogPost.setScheduledAt(request.getScheduledAt());
            }
            if (StringUtils.hasText(request.getPostType())) {
                blogPost.setPostType(request.getPostType());
            }

            // Sync author metadata if userId is present
            if (StringUtils.hasText(blogPost.getUserId())) {
                log.debug("Syncing author profile for userId: {}", blogPost.getUserId());
                UserProfileResponse profile = authServiceClient.getUserProfile(blogPost.getUserId());
                if (profile != null) {
                    blogPost.setAuthorEmail(profile.getEmail());
                    blogPost.setAuthorFirstName(profile.getFirstName());
                    blogPost.setAuthorLastName(profile.getLastName());
                    blogPost.setAuthorAvatarUrl(profile.getAvatarUrl());
                }
            }

            // Update manual overrides from request if provided
            if (StringUtils.hasText(request.getAuthorEmail())) {
                blogPost.setAuthorEmail(request.getAuthorEmail());
            }
            if (StringUtils.hasText(request.getAuthorFirstName())) {
                blogPost.setAuthorFirstName(request.getAuthorFirstName());
            }
            if (StringUtils.hasText(request.getAuthorLastName())) {
                blogPost.setAuthorLastName(request.getAuthorLastName());
            }
            if (StringUtils.hasText(request.getAuthorAvatarUrl())) {
                blogPost.setAuthorAvatarUrl(request.getAuthorAvatarUrl());
            }
            if (request.getCategories() != null) {
                blogPost.setCategories(request.getCategories());
            }
            if (request.getTags() != null) {
                blogPost.setTags(request.getTags());
            }
            if (request.getSectionSlug() != null) {
                blogPost.setSectionSlug(request.getSectionSlug());
            }
            if (request.getAllowComments() != null) {
                blogPost.setAllowComments(request.getAllowComments());
            }
            if (request.getAllowLikes() != null) {
                blogPost.setAllowLikes(request.getAllowLikes());
            }

            // Extract all NEW URLs from the updated JSON payload
            Set<String> newUrls = extractAllMediaUrlsFromPost(blogPost);
            
            Set<String> deletedUrls = new HashSet<>();

            Set<String> orphanedUrls = new HashSet<>(oldUrls);
            orphanedUrls.removeAll(newUrls);

            // AGGRESSIVE GARBAGE COLLECTION:
            // Find any legacy images that are sitting in the media array with a 'null' subtitle AND whose URLs are completely missing from the brand new JSON payload!
            if (blogPost.getMedia() != null) {
                List<String> validMediaIdsToDelete = blogPost.getMedia().stream()
                        .filter(m -> {
                            // Delete if it was explicitly removed from the JSON (Orphaned)
                            if (orphanedUrls.contains(normalizeUrl(m.getMediaUrl()))) return true;
                            
                            // Delete if it's a completely unmapped ghost image (URL completely absent from JSON + No Subtitle)
                            boolean isUsedInJson = newUrls.contains(normalizeUrl(m.getMediaUrl()));
                            boolean hasNoSubtitle = !StringUtils.hasText(m.getSubtitle());
                            return !isUsedInJson && hasNoSubtitle;
                        })
                        .map(PostMedia::getId)
                        .toList();
                        
                log.info("Garbage Collector auto-detecting {} unmapped or orphaned legacy images...", validMediaIdsToDelete.size());
                        
                for (String orphanId : validMediaIdsToDelete) {
                    try {
                        deletedUrls.add(deleteMediaFile(orphanId, blogPost, userId, isAdmin));
                        log.info("Successfully auto-deleted unmapped media: {}", orphanId);
                    } catch (Exception e) {
                        log.warn("Failed to auto-delete unmapped media {}: {}", orphanId, e.getMessage());
                    }
                }
            }

            // Handle media deletion
            if (Boolean.TRUE.equals(request.getDeleteAllMedia())) {
                deletedUrls.addAll(deleteAllMediaFiles(blogPost, userId, isAdmin));
            }

            if (request.getMediaSubtitlesToDelete() != null && !request.getMediaSubtitlesToDelete().isEmpty()) {
                deletedUrls.addAll(deleteMediaFilesBySubtitles(blogPost, request.getMediaSubtitlesToDelete(), userId, isAdmin));
            }

            if (request.getMediaIdsToDelete() != null && !request.getMediaIdsToDelete().isEmpty()) {
                for (String mediaId : request.getMediaIdsToDelete()) {
                    deletedUrls.add(deleteMediaFile(mediaId, blogPost, userId, isAdmin));
                }
            }

            // Handle new media uploads
            // Check if using new grouped structure or legacy flat structure
            CompletableFuture<List<PostMedia>> uploadFuture = null;

            if (request.getNewMediaGroups() != null && !request.getNewMediaGroups().isEmpty()
                    && request.getNewGroupedMediaFiles() != null && !request.getNewGroupedMediaFiles().isEmpty()) {
                // New grouped structure: multiple images per subtitle
                log.info("Using grouped media structure for update with {} groups and {} files",
                        request.getNewMediaGroups().size(), request.getNewGroupedMediaFiles().size());
                if (!Boolean.TRUE.equals(request.getAppendMedia())) {
                    List<String> subtitles = request.getNewMediaGroups().stream()
                            .map(MediaGroupRequest::getSubtitle)
                            .filter(Objects::nonNull)
                            .filter(s -> !s.trim().isEmpty())
                            .distinct()
                            .toList();
                    if (!subtitles.isEmpty()) {
                        deletedUrls.addAll(deleteMediaFilesBySubtitles(blogPost, subtitles, userId, isAdmin));
                    }
                }
                List<MultipartFile> copiedFiles = copyMultipartFiles(request.getNewGroupedMediaFiles());
                uploadFuture = asyncMediaUploadService.uploadGroupedMediaFilesAsync(postId,
                        request.getNewMediaGroups(), copiedFiles, userId);

            } else if (request.getNewMediaFiles() != null && !request.getNewMediaFiles().isEmpty()) {
                // Legacy flat structure: one subtitle per image
                log.info("Using legacy flat media structure for update with {} files",
                        request.getNewMediaFiles().size());
                if (!Boolean.TRUE.equals(request.getAppendMedia())) {
                    List<String> subtitles = request.getNewMediaSubtitles() == null ? List.of()
                            : request.getNewMediaSubtitles().stream()
                                    .filter(Objects::nonNull)
                                    .filter(s -> !s.trim().isEmpty())
                                    .distinct()
                                    .toList();
                    if (!subtitles.isEmpty()) {
                        deletedUrls.addAll(deleteMediaFilesBySubtitles(blogPost, subtitles, userId, isAdmin));
                    }
                }
                List<MultipartFile> copiedFiles = copyMultipartFiles(request.getNewMediaFiles());
                uploadFuture = asyncMediaUploadService.uploadMediaFilesAsync(postId, copiedFiles,
                        request.getNewMediaCaptions(),
                        request.getNewMediaDescriptions(),
                        request.getNewMediaSubtitles(),
                        userId);
            }

            // Wait for media uploads to complete (with timeout to avoid Cloudflare 60s limit).
            // Wait for media uploads to complete (parallelized inside the async service).
            if (uploadFuture != null) {
                try {
                    log.info("Waiting for parallel media uploads to complete (max 120 seconds)...");
                    List<PostMedia> uploadedMedia = uploadFuture.get(120, TimeUnit.SECONDS);
                    log.info("Media uploads completed successfully");

                    if (uploadedMedia != null && !uploadedMedia.isEmpty()) {
                        // Match with existing media to avoid duplicates in the same instance
                        if (blogPost.getMedia() == null) {
                            blogPost.setMedia(new ArrayList<>());
                        }
                        
                        Set<String> existingUrls = blogPost.getMedia().stream()
                                .map(PostMedia::getMediaUrl)
                                .collect(Collectors.toSet());

                        for (PostMedia media : uploadedMedia) {
                            if (!existingUrls.contains(media.getMediaUrl())) {
                                media.setPost(blogPost);
                                blogPost.getMedia().add(media);
                            }
                        }
                        log.info("Attached {} media items to blog post for response", uploadedMedia.size());
                    }

                } catch (TimeoutException e) {
                    log.warn("Media upload timeout after 120 seconds. Persistence will be handled in the background.");
                } catch (Exception e) {
                    log.error("Error waiting for media uploads: {}", e.getMessage(), e);
                }
            }

            cleanupDeletedUrlsFromPost(blogPost, deletedUrls);

            BlogPost updatedPost = blogPostRepository.saveAndFlush(blogPost);

            // Refresh to get updated media (with eager fetching)
            updatedPost = blogPostRepository.findByIdWithMedia(updatedPost.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Blog post not found after update"));

            updatedPost = applyUploadedMediaUrls(updatedPost);

            // Publish blog post updated event to Kafka
            try {
                String correlationId = MDC.get("correlationId");
                if (correlationId == null) {
                    correlationId = java.util.UUID.randomUUID().toString();
                }
                blogEventProducer.publishBlogPostUpdated(
                        updatedPost.getId(),
                        updatedPost.getTitle(),
                        correlationId);
            } catch (Exception e) {
                log.warn("Failed to publish blog post updated event for post {}: {}", updatedPost.getId(),
                        e.getMessage());
            }

            log.info("Blog post updated successfully with ID: {}", updatedPost.getId());

            // Fetch fresh profile for the author
            UserProfileResponse authorProfile = null;
            if (StringUtils.hasText(updatedPost.getUserId())) {
                authorProfile = authServiceClient.getUserProfile(updatedPost.getUserId());
            }
            // Initialize lazy collections to avoid LazyInitializationException during serialization
            initializeLazyCollections(updatedPost);

            return blogMapper.toBlogPostResponse(updatedPost, authorProfile);

        } catch (Exception e) {
            log.error("Error updating blog post: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update blog post: " + e.getMessage());
        }
    }

    @Override
    public BlogPostResponse publishBlogPost(String postId, String userId, boolean isAdmin) {
        log.info("Publishing blog post with ID: {} by user: {} (isAdmin: {})", postId, userId, isAdmin);

        BlogPost blogPost = blogPostRepository.findByIdWithMedia(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + postId));

        if (!blogPost.getUserId().equals(userId) && !isAdmin) {
            throw new ForbiddenException("You can only publish your own blog posts");
        }

        if ("PUBLISHED".equals(blogPost.getStatus())) {
            throw new BadRequestException("Blog post is already published");
        }

        blogPost.setStatus("PUBLISHED");
        blogPost.setPublishedAt(LocalDateTime.now());

        BlogPost publishedPost = blogPostRepository.saveAndFlush(blogPost);
        
        // Re-fetch to ensure all lazy collections are initialized if needed, 
        // though findByIdWithMedia already fetched media.
        publishedPost = blogPostRepository.findByIdWithMedia(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found after publishing"));

        // Publish blog post published event to Kafka
        try {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = java.util.UUID.randomUUID().toString();
            }
            blogEventProducer.publishBlogPostPublished(
                    publishedPost.getId(),
                    publishedPost.getTitle(),
                    publishedPost.getUserId(),
                    correlationId);
        } catch (Exception e) {
            log.warn("Failed to publish blog post published event for post {}: {}", publishedPost.getId(),
                    e.getMessage());
        }

        log.info("Blog post published successfully with ID: {}", publishedPost.getId());

        // Fetch fresh profile for the author
        UserProfileResponse authorProfile = null;
        if (StringUtils.hasText(publishedPost.getUserId())) {
            authorProfile = authServiceClient.getUserProfile(publishedPost.getUserId());
        }
        // Initialize lazy collections to avoid LazyInitializationException during serialization
        initializeLazyCollections(publishedPost);

        return blogMapper.toBlogPostResponse(publishedPost, authorProfile);
    }

    @Override
    public BlogPostResponse unpublishBlogPost(String postId, String userId, boolean isAdmin) {
        log.info("Unpublishing blog post with ID: {} by user: {} (isAdmin: {})", postId, userId, isAdmin);

        BlogPost blogPost = blogPostRepository.findByIdWithMedia(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + postId));

        if (!blogPost.getUserId().equals(userId) && !isAdmin) {
            throw new ForbiddenException("You can only unpublish your own blog posts");
        }

        if (!"PUBLISHED".equals(blogPost.getStatus())) {
            throw new BadRequestException("Blog post is not published");
        }

        blogPost.setStatus("DRAFT");
        blogPost.setPublishedAt(null);

        BlogPost unpublishedPost = blogPostRepository.saveAndFlush(blogPost);

        // Re-fetch to ensure consistency
        unpublishedPost = blogPostRepository.findByIdWithMedia(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found after unpublishing"));
        log.info("Blog post unpublished successfully with ID: {}", unpublishedPost.getId());

        // Fetch fresh profile for the author
        UserProfileResponse authorProfile = null;
        if (StringUtils.hasText(unpublishedPost.getUserId())) {
            authorProfile = authServiceClient.getUserProfile(unpublishedPost.getUserId());
        }
        // Initialize lazy collections to avoid LazyInitializationException during serialization
        initializeLazyCollections(unpublishedPost);

        return blogMapper.toBlogPostResponse(unpublishedPost, authorProfile);
    }

    @Override
    public void deleteBlogPost(String postId, String userId, boolean isAdmin) {
        log.info("Deleting blog post with ID: {} by user: {}", postId, userId);

        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + postId));

        if (!isAdmin && !blogPost.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own blog posts");
        }

        try {
            // Delete associated media files from S3
            List<PostMedia> mediaList = postMediaRepository.findByPostId(postId);
            for (PostMedia media : mediaList) {
                s3Helper.deleteFile(media.getMediaUrl());
            }

            // Delete blog post (cascade will handle related entities)
            blogPostRepository.delete(blogPost);

            log.info("Blog post deleted successfully with ID: {}", postId);
        } catch (Exception e) {
            log.error("Error deleting blog post: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete blog post: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void incrementViewCount(String postId) {
        log.debug("Incrementing view count for blog post: {}", postId);
        try {
            blogPostRepository.incrementViewCount(postId);
        } catch (Exception e) {
            log.warn("Failed to increment view count for post {}: {}", postId, e.getMessage());
        }
    }

    private int getNextDisplayOrder(String postId) {
        List<PostMedia> existing = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId);
        if (existing == null || existing.isEmpty()) {
            return 1;
        }
        PostMedia last = existing.get(existing.size() - 1);
        Integer lastOrder = last.getDisplayOrder();
        if (lastOrder != null) {
            return lastOrder + 1;
        }
        return existing.size() + 1;
    }

    /**
     * Uploads grouped media files where multiple images share the same subtitle.
     * Each group has a subtitle and multiple images with individual descriptions.
     * 
     * Structure:
     * - mediaGroups: Array of groups, each with subtitle and descriptions array
     * - groupedMediaFiles: Flat array of all image files in order (group1 images,
     * then group2 images, etc.)
     * 
     * Example:
     * Group 1: subtitle="Day 1", descriptions=["desc1", "desc2", "desc3"] -> 3
     * images
     * Group 2: subtitle="Day 2", descriptions=["desc4", "desc5"] -> 2 images
     * groupedMediaFiles: [img1, img2, img3, img4, img5]
     */

    /**
     * Copy MultipartFile content to byte arrays so they can be used after request
     * completes.
     * This is necessary because MultipartFile streams can't be read after the HTTP
     * request finishes.
     */
    private List<MultipartFile> copyMultipartFiles(List<MultipartFile> files) {
        List<MultipartFile> copiedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    byte[] content = file.getBytes();
                    String name = file.getName();
                    String originalFilename = file.getOriginalFilename();
                    String contentType = file.getContentType();

                    // Create a simple MultipartFile wrapper
                    MultipartFile copiedFile = new MultipartFile() {
                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public String getOriginalFilename() {
                            return originalFilename;
                        }

                        @Override
                        public String getContentType() {
                            return contentType;
                        }

                        @Override
                        public boolean isEmpty() {
                            return content == null || content.length == 0;
                        }

                        @Override
                        public long getSize() {
                            return content != null ? content.length : 0;
                        }

                        @Override
                        public byte[] getBytes() throws IOException {
                            return content;
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            return new ByteArrayInputStream(content);
                        }

                        @Override
                        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                            java.nio.file.Files.write(dest.toPath(), content);
                        }
                    };
                    copiedFiles.add(copiedFile);
                } catch (IOException e) {
                    log.error("Error copying file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                    // Continue with other files even if one fails
                }
            }
        }
        return copiedFiles;
    }

    private BlogPost applyUploadedMediaUrls(BlogPost blogPost) {
        if (blogPost == null || blogPost.getMedia() == null || blogPost.getMedia().isEmpty()) {
            return blogPost;
        }

        List<PostMedia> sortedMedia = blogPost.getMedia().stream()
                .sorted((a, b) -> {
                    Integer ao = a.getDisplayOrder();
                    Integer bo = b.getDisplayOrder();
                    if (ao == null && bo == null)
                        return 0;
                    if (ao == null)
                        return 1;
                    if (bo == null)
                        return -1;
                    return ao.compareTo(bo);
                })
                .toList();

        Map<String, String> subtitleToUrl = new HashMap<>();
        Map<String, String> captionToUrl = new HashMap<>();
        for (PostMedia media : sortedMedia) {
            if (media == null)
                continue;
            String url = normalizeUrl(media.getMediaUrl());
            if (!StringUtils.hasText(url))
                continue;
            String subtitle = normalizeKey(media.getSubtitle());
            if (StringUtils.hasText(subtitle)) {
                subtitleToUrl.putIfAbsent(subtitle, url);
            }
            String caption = normalizeKey(media.getCaption());
            if (StringUtils.hasText(caption)) {
                captionToUrl.putIfAbsent(caption, url);
            }
        }

        Set<String> usedUrls = new HashSet<>();

        JsonNode featuredImage = blogPost.getFeaturedImage();
        // FIX: If featuredImage is null but media files were uploaded, auto-create an
        // empty object so the first uploaded image is used as the featured image.
        if (featuredImage == null || featuredImage.isNull()) {
            featuredImage = JsonNodeFactory.instance.objectNode();
            blogPost.setFeaturedImage(featuredImage);
        }
        if (featuredImage != null && featuredImage.isObject()) {
            String existing = normalizeUrl(textOrNull(featuredImage.get("url")));
            if (StringUtils.hasText(existing))
                usedUrls.add(existing);
        }

        JsonNode contentBlocks = blogPost.getContentBlocks();
        if (contentBlocks != null && contentBlocks.isArray()) {
            for (JsonNode blockNode : contentBlocks) {
                if (!blockNode.isObject())
                    continue;
                JsonNode imageNode = blockNode.get("image");
                if (imageNode != null && imageNode.isObject()) {
                    String existing = normalizeUrl(textOrNull(imageNode.get("url")));
                    if (StringUtils.hasText(existing))
                        usedUrls.add(existing);
                }
                String type = normalizeKey(textOrNull(blockNode.get("type")));
                if ("gallery".equals(type)) {
                    JsonNode itemsNode = blockNode.get("items");
                    if (itemsNode != null && itemsNode.isArray()) {
                        for (JsonNode itemNode : itemsNode) {
                            if (!itemNode.isObject())
                                continue;
                            String existing = normalizeUrl(textOrNull(itemNode.get("url")));
                            if (StringUtils.hasText(existing))
                                usedUrls.add(existing);
                        }
                    }
                }
            }
        }

        boolean hasAnyUrl = sortedMedia.stream()
                .map(m -> normalizeUrl(m != null ? m.getMediaUrl() : null))
                .anyMatch(StringUtils::hasText);
        if (!hasAnyUrl) {
            return blogPost;
        }



        java.util.function.Function<String, String> findByKey = (key) -> {
            String k = normalizeKey(key);
            if (!StringUtils.hasText(k))
                return null;
            String url = subtitleToUrl.get(k);
            if (StringUtils.hasText(url) && !usedUrls.contains(url))
                return url;
            url = captionToUrl.get(k);
            if (StringUtils.hasText(url) && !usedUrls.contains(url))
                return url;
            return null;
        };

        if (featuredImage != null && featuredImage.isObject()) {
            ObjectNode fi = ((ObjectNode) featuredImage).deepCopy();
            String url = normalizeUrl(textOrNull(fi.get("url")));
            if (!StringUtils.hasText(url)) {
                log.debug("Featured image URL is missing, attempting to pick from uploaded media...");
                String picked = findByKey.apply("cover");
                if (!StringUtils.hasText(picked))
                    picked = findByKey.apply("featured");


                if (StringUtils.hasText(picked)) {
                    log.info("Auto-populated featured image URL with: {}", picked);
                    fi.put("url", picked);
                    usedUrls.add(picked);
                } else {
                    log.warn("Could not find any suitable uploaded image to use as featured image");
                }
            } else {
                log.debug("Featured image already has URL: {}", url);
                usedUrls.add(url);
            }
            blogPost.setFeaturedImage(fi);
        }

        if (contentBlocks != null && contentBlocks.isArray()) {
            ArrayNode blocks = ((ArrayNode) contentBlocks).deepCopy();
            for (int i = 0; i < blocks.size(); i++) {
                JsonNode node = blocks.get(i);
                if (!node.isObject())
                    continue;
                ObjectNode block = (ObjectNode) node;

                JsonNode imageNode = block.get("image");
                if (imageNode != null && imageNode.isObject()) {
                    ObjectNode image = ((ObjectNode) imageNode).deepCopy();
                    String imageUrl = normalizeUrl(textOrNull(image.get("url")));
                    if (!StringUtils.hasText(imageUrl)) {
                        String picked = findByKey.apply(textOrNull(block.get("id")));
                        if (!StringUtils.hasText(picked))
                            picked = findByKey.apply(textOrNull(block.get("title")));

                        if (StringUtils.hasText(picked)) {
                            image.put("url", picked);
                            usedUrls.add(picked);
                        }
                    } else if (imageUrl != null && !imageUrl.equals(textOrNull(image.get("url")))) {
                        image.put("url", imageUrl);
                        usedUrls.add(imageUrl);
                    }
                    block.set("image", image);
                }

                String type = normalizeKey(textOrNull(block.get("type")));
                if ("gallery".equals(type)) {
                    JsonNode itemsNode = block.get("items");
                    if (itemsNode != null && itemsNode.isArray()) {
                        ArrayNode items = ((ArrayNode) itemsNode).deepCopy();
                        for (int j = 0; j < items.size(); j++) {
                            JsonNode itemNode = items.get(j);
                            if (!itemNode.isObject())
                                continue;
                            ObjectNode item = (ObjectNode) itemNode;
                            String itemUrl = normalizeUrl(textOrNull(item.get("url")));
                            if (StringUtils.hasText(itemUrl)) {
                                if (!itemUrl.equals(textOrNull(item.get("url")))) {
                                    item.put("url", itemUrl);
                                }
                                usedUrls.add(itemUrl);
                            }
                            items.set(j, item);
                        }
                        block.set("items", items);
                    }
                }

                if ("imagebulletsection".equals(type)) {
                    JsonNode bulletsNode = block.get("bullets");
                    if (bulletsNode != null && bulletsNode.isArray()) {
                        ArrayNode bulletsArray = (ArrayNode) bulletsNode;
                        boolean allText = true;
                        for (JsonNode b : bulletsArray) {
                            if (b == null || b.isNull())
                                continue;
                            if (!b.isTextual()) {
                                allText = false;
                                break;
                            }
                        }
                        if (allText) {
                            ArrayNode bulletItems = JsonNodeFactory.instance.arrayNode();
                            for (JsonNode b : bulletsArray) {
                                if (b == null || b.isNull())
                                    continue;
                                ObjectNode item = JsonNodeFactory.instance.objectNode();
                                item.put("text", b.asText());
                                bulletItems.add(item);
                            }
                            block.set("bulletItems", bulletItems);
                        } else {
                            JsonNode existingBulletItems = block.get("bulletItems");
                            if (existingBulletItems == null || existingBulletItems.isNull()) {
                                block.set("bulletItems", bulletsNode);
                            }
                        }
                    }
                }

                blocks.set(i, block);
            }
            blogPost.setContentBlocks(blocks);
        }

        BlogPost saved = blogPostRepository.save(blogPost);
        entityManager.flush();
        return blogPostRepository.findByIdWithMedia(saved.getId()).orElse(saved);
    }

    private String normalizeUrl(String value) {
        if (value == null)
            return null;
        String v = value.trim();
        if (v.length() >= 2 && v.startsWith("`") && v.endsWith("`")) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v;
    }

    private String normalizeKey(String value) {
        if (value == null)
            return null;
        String v = value.trim().toLowerCase();
        return v.isEmpty() ? null : v;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
    
    private Set<String> extractAllMediaUrlsFromPost(BlogPost blogPost) {
        Set<String> urls = new HashSet<>();
        
        JsonNode featuredImage = blogPost.getFeaturedImage();
        if (featuredImage != null && featuredImage.isObject()) {
            String url = normalizeUrl(textOrNull(featuredImage.get("url")));
            if (StringUtils.hasText(url)) urls.add(url);
        }
        
        JsonNode contentBlocks = blogPost.getContentBlocks();
        if (contentBlocks != null && contentBlocks.isArray()) {
            for (JsonNode node : contentBlocks) {
                if (!node.isObject()) continue;
                
                // Check block images
                JsonNode imageNode = node.get("image");
                if (imageNode != null && imageNode.isObject()) {
                    String url = normalizeUrl(textOrNull(imageNode.get("url")));
                    if (StringUtils.hasText(url)) urls.add(url);
                }
                
                // Check gallery items
                JsonNode itemsNode = node.get("items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode item : itemsNode) {
                        if (item != null && item.isObject()) {
                            String url = normalizeUrl(textOrNull(item.get("url")));
                            if (StringUtils.hasText(url)) urls.add(url);
                        }
                    }
                }
            }
        }
        return urls;
    }

    private Set<String> deleteAllMediaFiles(BlogPost blogPost, String userId, boolean isAdmin) {
        Set<String> deletedUrls = new HashSet<>();
        List<PostMedia> mediaList = postMediaRepository.findByPostId(blogPost.getId());
        for (PostMedia media : mediaList) {
            deletedUrls.add(deleteMediaFile(media.getId(), blogPost, userId, isAdmin));
        }
        return deletedUrls;
    }

    private Set<String> deleteMediaFilesBySubtitles(BlogPost blogPost, List<String> subtitles, String userId,
            boolean isAdmin) {
        Set<String> deletedUrls = new HashSet<>();
        List<String> normalized = subtitles.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .distinct()
                .toList();
        if (normalized.isEmpty())
            return deletedUrls;
        List<PostMedia> mediaList = postMediaRepository.findByPostIdAndSubtitleIn(blogPost.getId(), normalized);
        for (PostMedia media : mediaList) {
            deletedUrls.add(deleteMediaFile(media.getId(), blogPost, userId, isAdmin));
        }
        return deletedUrls;
    }

    private String deleteMediaFile(String mediaId, BlogPost blogPost, String userId, boolean isAdmin) {
        PostMedia media = postMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        if (!media.getPost().getId().equals(blogPost.getId())) {
            throw new BadRequestException("Media does not belong to this blog post");
        }

        if (!blogPost.getUserId().equals(userId) && !isAdmin) {
            throw new ForbiddenException("You don't have permission to delete media for this post");
        }

        try {
            String url = media.getMediaUrl();
            // Delete from S3
            s3Helper.deleteFile(url);
            
            // Remove from the blog post's media collection to avoid ObjectDeletedException 
            // during saveAndFlush (since orphanRemoval = true is set)
            if (blogPost.getMedia() != null) {
                blogPost.getMedia().removeIf(m -> m.getId().equals(mediaId));
            }
            
            // Explicitly delete from repository as well
            postMediaRepository.delete(media);
            return url;
        } catch (Exception e) {
            log.error("Error deleting media file: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete media file: " + e.getMessage());
        }
    }

    private void cleanupDeletedUrlsFromPost(BlogPost blogPost, Set<String> deletedUrls) {
        if (deletedUrls == null || deletedUrls.isEmpty()) return;
        
        JsonNode featuredImage = blogPost.getFeaturedImage();
        if (featuredImage != null && featuredImage.isObject()) {
            String url = normalizeUrl(textOrNull(featuredImage.get("url")));
            if (url != null && deletedUrls.contains(url)) {
                ObjectNode fi = ((ObjectNode) featuredImage).deepCopy();
                fi.remove("url");
                blogPost.setFeaturedImage(fi);
                log.info("Cleared deleted URL from featured image");
            }
        }
        
        JsonNode contentBlocks = blogPost.getContentBlocks();
        if (contentBlocks != null && contentBlocks.isArray()) {
            ArrayNode blocks = ((ArrayNode) contentBlocks).deepCopy();
            boolean changed = false;
            for (int i = 0; i < blocks.size(); i++) {
                JsonNode node = blocks.get(i);
                if (!node.isObject()) continue;
                ObjectNode block = (ObjectNode) node;
                
                JsonNode imageNode = block.get("image");
                if (imageNode != null && imageNode.isObject()) {
                    String url = normalizeUrl(textOrNull(imageNode.get("url")));
                    if (url != null && deletedUrls.contains(url)) {
                        ObjectNode image = ((ObjectNode) imageNode).deepCopy();
                        image.remove("url");
                        block.set("image", image);
                        changed = true;
                    }
                }
            }
            if (changed) {
                blogPost.setContentBlocks(blocks);
                log.info("Cleared deleted URLs from content blocks");
            }
        }
    }

    private String determineMediaType(String contentType) {
        if (contentType == null)
            return "file";

        if (contentType.startsWith("image/"))
            return "image";
        if (contentType.startsWith("video/"))
            return "video";
        if (contentType.startsWith("audio/"))
            return "audio";
        return "file";
    }

    private Map<String, UserProfileResponse> fetchProfilesForPosts(List<BlogPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return new HashMap<>();
        }

        List<String> userIds = posts.stream()
                .map(BlogPost::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            return authServiceClient.getUserProfiles(userIds);
        } catch (Exception e) {
            log.error("Failed to batch fetch user profiles for posts: {}", e.getMessage());
            return new HashMap<>();
        }
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

    private Pageable createPageable(PaginationRequest paginationRequest) {
        Sort sort = Sort.by(Sort.Direction.fromString(paginationRequest.getSortDirection()),
                paginationRequest.getSortBy());
        return PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize(), sort);
    }

    @Override
    public void likePost(String postId, String userId) {
        log.info("Liking post: {} by user: {}", postId, userId);

        // Validate blog post exists
        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + postId));

        // Check if post allows likes
        if (!blogPost.getAllowLikes()) {
            throw new BadRequestException("This post does not allow likes");
        }

        // Delegate to PostLikeService
        postLikeService.likePost(in.mapmytour.blog.dto.request.postlike.LikePostRequest.builder()
                .postId(postId)
                .userId(userId)
                .build());
    }

    @Override
    public void unlikePost(String postId, String userId) {
        log.info("Unliking post: {} by user: {}", postId, userId);
        postLikeService.unlikePost(postId, userId);
    }

    @Override
    public List<PostLikeResponse> getPostLikes(String postId) {
        log.info("Getting likes for post: {}", postId);
        return postLikeService.getPostLikes(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BlogPostSummaryResponse> getMyLikedPosts(String userId,
            PaginationRequest paginationRequest) {
        log.info("Getting liked posts for user: {} with pagination: {}", userId, paginationRequest);

        Pageable pageable = createPageable(paginationRequest);
        Page<BlogPost> likedPostsPage = blogPostRepository.findLikedPostsByUserId(userId, pageable);

        // Initialize lazy collections
        initializeLazyCollections(likedPostsPage.getContent());

        Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(likedPostsPage.getContent());
        List<BlogPostSummaryResponse> likedPosts = likedPostsPage.getContent().stream()
                .map(post -> blogMapper.toBlogPostSummaryResponse(post, profileMap.get(post.getUserId())))
                .collect(java.util.stream.Collectors.toList());

        return PaginatedResponse.<BlogPostSummaryResponse>builder()
                .content(likedPosts)
                .page(likedPostsPage.getNumber())
                .size(likedPostsPage.getSize())
                .totalElements(likedPostsPage.getTotalElements())
                .totalPages(likedPostsPage.getTotalPages())
                .first(likedPostsPage.isFirst())
                .last(likedPostsPage.isLast())
                .empty(likedPostsPage.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BlogPostSummaryResponse> getAllBlogPostsByStatus(String status, String userId,
            boolean isAdmin, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<BlogPost> blogPosts;
        if (isAdmin) {
            if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status)) {
                blogPosts = blogPostRepository.findAll(pageable);
            } else {
                blogPosts = blogPostRepository.findByStatus(status.toUpperCase(), pageable);
            }
        } else {
            if ("DRAFT".equalsIgnoreCase(status)) {
                blogPosts = blogPostRepository.findByStatusAndUserId("DRAFT", userId, pageable);
            } else if ("PUBLISHED".equalsIgnoreCase(status)) {
                blogPosts = blogPostRepository.findByStatus("PUBLISHED", pageable);
            } else {
                blogPosts = blogPostRepository.findPublishedOrUserPosts(userId, pageable);
            }
        }

        // Initialize lazy collections
        initializeLazyCollections(blogPosts.getContent());

        Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(blogPosts.getContent());
        Page<BlogPostSummaryResponse> responsePage = blogPosts
                .map(post -> blogMapper.toBlogPostSummaryResponse(post, profileMap.get(post.getUserId())));
        return blogMapper.toPaginatedResponse(responsePage);
    }

    /**
     * Helper to initialize lazy-loaded collections on a BlogPost entity.
     * This avoids LazyInitializationException when these collections are accessed
     * outside of the transaction/session (e.g., during DTO mapping or serialization).
     */
    private void initializeLazyCollections(BlogPost post) {
        if (post == null) {
            return;
        }
        // Accessing size() of LB (Lazy Bag) triggers initialization within the active
        // session
        if (post.getCategories() != null) {
            post.getCategories().size();
        }
        if (post.getTags() != null) {
            post.getTags().size();
        }
        if (post.getMedia() != null) {
            post.getMedia().size();
        }
        // Initialize comments as well if they might be accessed by the mapper
        if (post.getComments() != null) {
            post.getComments().size();
        }
    }

    /**
     * Helper to initialize lazy-loaded collections on a list of BlogPost entities.
     */
    private void initializeLazyCollections(List<BlogPost> posts) {
        if (posts == null) {
            return;
        }
        posts.forEach(this::initializeLazyCollections);
    }

    private List<BlogPostSummaryResponse> fetchRelatedPosts(BlogPost post, int limit) {
        try {
            List<String> categories = post.getCategories();
            List<String> tags = post.getTags();
            
            Set<BlogPost> relatedSet = new HashSet<>();
            
            // 1. Try by category
            if (categories != null && !categories.isEmpty()) {
                for (String cat : categories) {
                    Page<BlogPost> catPosts = blogPostRepository.findByCategory(cat, PageRequest.of(0, limit + 1));
                    relatedSet.addAll(catPosts.getContent());
                }
            }
            
            // 2. Try by tags if not enough
            if (relatedSet.size() <= 1 && tags != null && !tags.isEmpty()) { // <= 1 because it includes the current post
                for (String tag : tags) {
                    Page<BlogPost> tagPosts = blogPostRepository.findByTag(tag, PageRequest.of(0, limit + 1));
                    relatedSet.addAll(tagPosts.getContent());
                }
            }
            
            // Filter out current post and limit
            List<BlogPost> relatedList = relatedSet.stream()
                .filter(p -> !p.getId().equals(post.getId()))
                .filter(p -> "PUBLISHED".equals(p.getStatus()))
                .limit(limit)
                .collect(Collectors.toList());
                
            if (relatedList.isEmpty()) {
                return Collections.emptyList();
            }
            
            Map<String, UserProfileResponse> profileMap = fetchProfilesForPosts(relatedList);
            return relatedList.stream()
                .map(p -> blogMapper.toBlogPostSummaryResponse(p, profileMap.get(p.getUserId())))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch related posts for {}: {}", post.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    // =========================================================================
    // P0-1 / P0-3: Reading-time auto-computation + dual HTML + Markdown strip
    // Used for BOTH backend readingTime field calc AND mobile auto-suggest
    // (mobile replicates same regex logic in ReaderModeHelpers for parity)
    // =========================================================================

    private static final java.util.regex.Pattern HTML_TAG_RE =
            java.util.regex.Pattern.compile("<[^>]+>", java.util.regex.Pattern.MULTILINE);
    private static final java.util.regex.Pattern MD_LINK_RE =
            java.util.regex.Pattern.compile("!?\\[[^\\]]*\\]\\([^)]*\\)");
    private static final java.util.regex.Pattern MD_HEADING_RE =
            java.util.regex.Pattern.compile("^#{1,6}\\s+", java.util.regex.Pattern.MULTILINE);
    private static final java.util.regex.Pattern MD_FORMAT_RE =
            java.util.regex.Pattern.compile("(\\*{1,3}|_{1,3}|`{1,3}|~~|> |\\| |- )");
    private static final java.util.regex.Pattern WHITESPACE_RE =
            java.util.regex.Pattern.compile("\\s+");

    static String stripHtmlAndMarkdown(String raw) {
        if (raw == null) return "";
        String s = raw;
        // Decode common HTML entities first so word boundaries aren't broken
        s = s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
        // Pass 1: HTML tags (incl. attributes, self-closing, comments)
        s = HTML_TAG_RE.matcher(s).replaceAll(" ");
        // Pass 2: Markdown links + images [text](url) / ![alt](url)
        s = MD_LINK_RE.matcher(s).replaceAll(m -> m.group(0).startsWith("!") ? "" : " " + m.group(0).replaceAll("^!*\\[([^\\]]*)\\]\\(.*\\)$", "$1") + " ");
        // Pass 3: Markdown headings (#, ##, ###, etc.)
        s = MD_HEADING_RE.matcher(s).replaceAll("");
        // Pass 4: Markdown formatting chars (*, _, `, ~~, blockquote >, tables, list bullets)
        s = MD_FORMAT_RE.matcher(s).replaceAll(" ");
        // Collapse whitespace
        s = WHITESPACE_RE.matcher(s).replaceAll(" ").trim();
        return s;
    }

    private static final int WORDS_PER_MINUTE = 200;

    /**
     * Auto-compute readingTime minutes using stripped word count @ 200 wpm.
     * Returns null for empty content (caller can decide fallback).
     */
    static Integer computeReadingTime(String content) {
        final String stripped = stripHtmlAndMarkdown(content);
        if (stripped.isEmpty()) return null;
        final int words = stripped.split("\\s+").length;
        return Math.max(1, (int) Math.ceil((double) words / WORDS_PER_MINUTE));
    }
}
