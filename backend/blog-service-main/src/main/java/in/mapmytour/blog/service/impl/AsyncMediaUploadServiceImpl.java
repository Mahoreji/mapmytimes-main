package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.blogpost.MediaGroupRequest;
import in.mapmytour.blog.helper.S3Helper;
import in.mapmytour.blog.repository.PostMediaRepository;
import in.mapmytour.blog.repository.BlogPostRepository;
import in.mapmytour.blog.entity.BlogPost;
import in.mapmytour.blog.entity.PostMedia;
import in.mapmytour.blog.service.AsyncMediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncMediaUploadServiceImpl implements AsyncMediaUploadService {

    private final PostMediaRepository postMediaRepository;
    private final S3Helper s3Helper;
    private final BlogPostRepository blogPostRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final Executor mediaUploadExecutor;

    @Override
    @Async("mediaUploadExecutor")
    public CompletableFuture<List<PostMedia>> uploadGroupedMediaFilesAsync(String postId, 
                                                                 List<MediaGroupRequest> mediaGroups, 
                                                                 List<MultipartFile> allMediaFiles, 
                                                                 String userId) {
        try {
            log.info("Starting parallel async upload of {} grouped media files for post {}", 
                    allMediaFiles.size(), postId);
            
            // Note: Parallelization happens inside the method
            List<PostMedia> uploadedMedia = uploadGroupedMediaFilesParallel(postId, mediaGroups, allMediaFiles, userId);
            
            log.info("Completed parallel async upload of grouped media files for post {}", postId);
            return CompletableFuture.completedFuture(uploadedMedia);
        } catch (Exception e) {
            log.error("Error in async upload of grouped media files for post {}: {}", postId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async("mediaUploadExecutor")
    public CompletableFuture<List<PostMedia>> uploadMediaFilesAsync(String postId, 
                                                          List<MultipartFile> mediaFiles, 
                                                          List<String> captions, 
                                                          List<String> descriptions, 
                                                          List<String> subtitles, 
                                                          String userId) {
        try {
            log.info("Starting parallel async upload of {} media files for post {}", 
                    mediaFiles.size(), postId);
            
            // Note: Parallelization happens inside the method
            List<PostMedia> uploadedMedia = uploadMediaFilesParallel(postId, mediaFiles, captions, descriptions, subtitles, userId);
            
            log.info("Completed parallel async upload of media files for post {}", postId);
            return CompletableFuture.completedFuture(uploadedMedia);
        } catch (Exception e) {
            log.error("Error in async upload of media files for post {}: {}", postId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Uploads grouped media files in parallel.
     */
    private List<PostMedia> uploadGroupedMediaFilesParallel(String postId, 
                                                 List<MediaGroupRequest> mediaGroups, 
                                                 List<MultipartFile> allMediaFiles, 
                                                 String userId) {
        List<CompletableFuture<PostMedia>> futures = new ArrayList<>();
        int globalDisplayOrder = 1;
        int fileIndex = 0;
        
        for (int groupIndex = 0; groupIndex < mediaGroups.size(); groupIndex++) {
            MediaGroupRequest group = mediaGroups.get(groupIndex);
            String subtitle = group.getSubtitle();
            List<String> descriptions = group.getDescriptions();
            List<String> captions = group.getCaptions();
            
            int imagesInGroup = (descriptions != null && !descriptions.isEmpty()) ? descriptions.size() : 
                                (captions != null && !captions.isEmpty()) ? captions.size() : 1;
            
            for (int imageIndex = 0; imageIndex < imagesInGroup && fileIndex < allMediaFiles.size(); imageIndex++) {
                final int currentFileIndex = fileIndex;
                final int currentGroupIndex = groupIndex;
                final int currentOrder = globalDisplayOrder++;
                final String currentDescription = (descriptions != null && descriptions.size() > imageIndex) ? descriptions.get(imageIndex) : null;
                final String currentCaption = (captions != null && captions.size() > imageIndex) ? captions.get(imageIndex) : null;
                
                MultipartFile file = allMediaFiles.get(currentFileIndex);
                if (file != null && !file.isEmpty()) {
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        try {
                            String mediaUrl = s3Helper.uploadFile(file, "blog-posts/" + postId);
                            return PostMedia.builder()
                                    .mediaUrl(mediaUrl)
                                    .mediaType(determineMediaType(file.getContentType()))
                                    .caption(currentCaption)
                                    .description(currentDescription)
                                    .subtitle(subtitle)
                                    .subtitleGroupIndex(currentGroupIndex)
                                    .userId(userId)
                                    .displayOrder(currentOrder)
                                    .build();
                        } catch (Exception e) {
                            log.error("S3 upload failed for file at index {}: {}", currentFileIndex, e.getMessage());
                            return null;
                        }
                    }, mediaUploadExecutor));
                }
                fileIndex++;
            }
        }
        
        // Process any remaining files that weren't caught by the group definitions
        while (fileIndex < allMediaFiles.size()) {
            final int currentFileIndex = fileIndex;
            final int currentOrder = globalDisplayOrder++;
            MultipartFile file = allMediaFiles.get(currentFileIndex);
            if (file != null && !file.isEmpty()) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        String mediaUrl = s3Helper.uploadFile(file, "blog-posts/" + postId);
                        return PostMedia.builder()
                                .mediaUrl(mediaUrl)
                                .mediaType(determineMediaType(file.getContentType()))
                                .userId(userId)
                                .displayOrder(currentOrder)
                                .build();
                    } catch (Exception e) {
                        log.error("S3 upload failed for extra file at index {}: {}", currentFileIndex, e.getMessage());
                        return null;
                    }
                }, mediaUploadExecutor));
            }
            fileIndex++;
        }
        
        List<PostMedia> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
        
        // Persistence fallback: If this method is called within an async thread that might outlive
        // the main request, we MUST ensure the media is saved if it hasn't been already.
        // BlogPostServiceImpl handles saving if it doesn't time out, but we'll add a safety save here.
        saveMediaSafely(postId, results);

        return results;
    }

    /**
     * Uploads flat media files in parallel.
     */
    private List<PostMedia> uploadMediaFilesParallel(String postId, 
                                           List<MultipartFile> mediaFiles, 
                                           List<String> captions, 
                                           List<String> descriptions, 
                                           List<String> subtitles, 
                                           String userId) {
        List<CompletableFuture<PostMedia>> futures = new ArrayList<>();
        
        for (int i = 0; i < mediaFiles.size(); i++) {
            final int index = i;
            final MultipartFile file = mediaFiles.get(index);
            if (file != null && !file.isEmpty()) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        String mediaUrl = s3Helper.uploadFile(file, "blog-posts/" + postId);
                        return PostMedia.builder()
                                .mediaUrl(mediaUrl)
                                .mediaType(determineMediaType(file.getContentType()))
                                .caption((captions != null && captions.size() > index) ? captions.get(index) : null)
                                .description((descriptions != null && descriptions.size() > index) ? descriptions.get(index) : null)
                                .subtitle((subtitles != null && subtitles.size() > index) ? subtitles.get(index) : null)
                                .subtitleGroupIndex(null)
                                .userId(userId)
                                .displayOrder(index + 1)
                                .build();
                    } catch (Exception e) {
                        log.error("S3 upload failed for file {}: {}", index, e.getMessage());
                        return null;
                    }
                }, mediaUploadExecutor));
            }
        }
        
        List<PostMedia> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        saveMediaSafely(postId, results);
        
        return results;
    }

    @org.springframework.transaction.annotation.Transactional
    protected void saveMediaSafely(String postId, List<PostMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) return;
        
        try {
            // Check if media already exists for this post to avoid duplicates
            // This is a simple safety check. In production, we'd use a more robust idempotency key.
            long existingCount = postMediaRepository.countByPostId(postId);
            if (existingCount >= mediaList.size()) {
                log.info("Media for post {} appears to be already persisted (count: {}). Skipping background save.", 
                        postId, existingCount);
                return;
            }

            // We need a BlogPost proxy or real entity to attach
            log.info("Background persisting {} media items for post {}", mediaList.size(), postId);
            
            // Re-fetch post in this transaction
            Optional<BlogPost> postOpt = blogPostRepository.findById(postId);
            if (postOpt.isPresent()) {
                BlogPost post = postOpt.get();
                for (PostMedia media : mediaList) {
                    media.setPost(post);
                }
                postMediaRepository.saveAll(mediaList);
                log.info("Background persistence successful for post {}", postId);
            } else {
                log.warn("Could not find post {} for background media persistence", postId);
            }
        } catch (Exception e) {
            log.warn("Background persistence failed (expected if main thread already saved it): {}", e.getMessage());
        }
    }

    private String determineMediaType(String contentType) {
        if (contentType == null) return "file";

        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("audio/")) return "audio";
        return "file";
    }
}

