package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.postmedia.UpdateMediaRequest;
import in.mapmytour.blog.dto.request.postmedia.UploadMediaRequest;
import in.mapmytour.blog.dto.response.postmedia.PostMediaResponse;
import in.mapmytour.blog.entity.BlogPost;
import in.mapmytour.blog.entity.PostMedia;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.exception.ForbiddenException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.helper.S3Helper;
import in.mapmytour.blog.repository.BlogPostRepository;
import in.mapmytour.blog.repository.PostMediaRepository;
import in.mapmytour.blog.service.PostMediaService;
import in.mapmytour.blog.utils.BlogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostMediaServiceImpl implements PostMediaService {

    private final PostMediaRepository postMediaRepository;
    private final BlogPostRepository blogPostRepository;
    private final S3Helper s3Helper;
    private final BlogMapper blogMapper;

    @Override
    public PostMediaResponse uploadMedia(UploadMediaRequest request) {
        log.info("Uploading media for post: {} by user: {}", request.getPostId(), request.getUserId());

        // Validate blog post exists
        BlogPost blogPost = blogPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + request.getPostId()));

        // Validate media file
        if (request.getMediaFile() == null || request.getMediaFile().isEmpty()) {
            throw new BadRequestException("Media file is required");
        }

        try {
            // Upload file to S3
            String mediaUrl = s3Helper.uploadFile(request.getMediaFile(), "blog-posts/" + request.getPostId());
            String mediaType = determineMediaType(request.getMediaFile().getContentType());

            // Set display order if not provided
            Integer displayOrder = request.getDisplayOrder();
            if (displayOrder == null) {
                long mediaCount = postMediaRepository.countByPostId(request.getPostId());
                displayOrder = (int) mediaCount + 1;
            }

            PostMedia media = PostMedia.builder()
                    .post(blogPost)
                    .mediaUrl(mediaUrl)
                    .mediaType(mediaType)
                    .caption(request.getCaption())
                    .description(request.getDescription())
                    .subtitle(request.getSubtitle())
                    .userId(request.getUserId())
                    .displayOrder(displayOrder)
                    .build();

            PostMedia savedMedia = postMediaRepository.save(media);
            log.info("Media uploaded successfully with ID: {}", savedMedia.getId());

            return blogMapper.toPostMediaResponse(savedMedia);
        } catch (Exception e) {
            log.error("Error uploading media: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to upload media: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PostMediaResponse getMedia(String mediaId) {
        log.info("Fetching media with ID: {}", mediaId);

        PostMedia media = postMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        return blogMapper.toPostMediaResponse(media);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostMediaResponse> getPostMedia(String postId) {
        log.info("Fetching media for post: {}", postId);

        // Validate blog post exists
        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId);
        return mediaList.stream()
                .map(blogMapper::toPostMediaResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PostMediaResponse updateMedia(String mediaId, UpdateMediaRequest request, String userId) {
        log.info("Updating media with ID: {} by user: {}", mediaId, userId);

        PostMedia media = postMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        // Check if user owns the media
        if (!media.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only update your own media files");
        }

        try {
            // Update caption if provided
            if (request.getCaption() != null) {
                media.setCaption(request.getCaption());
            }

            // Update description if provided
            if (request.getDescription() != null) {
                media.setDescription(request.getDescription());
            }

            // Update subtitle if provided
            if (request.getSubtitle() != null) {
                media.setSubtitle(request.getSubtitle());
            }

            // Update display order if provided
            if (request.getDisplayOrder() != null) {
                media.setDisplayOrder(request.getDisplayOrder());
            }

            // Update media file if provided
            if (request.getNewMediaFile() != null && !request.getNewMediaFile().isEmpty()) {
                // Delete old file
                s3Helper.deleteFile(media.getMediaUrl());

                // Upload new file
                String newMediaUrl = s3Helper.uploadFile(request.getNewMediaFile(), "blog-posts/" + media.getPost().getId());
                String newMediaType = determineMediaType(request.getNewMediaFile().getContentType());

                media.setMediaUrl(newMediaUrl);
                media.setMediaType(newMediaType);
            }

            PostMedia updatedMedia = postMediaRepository.save(media);
            log.info("Media updated successfully with ID: {}", updatedMedia.getId());

            return blogMapper.toPostMediaResponse(updatedMedia);
        } catch (Exception e) {
            log.error("Error updating media: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update media: " + e.getMessage());
        }
    }

    @Override
    public void deleteMedia(String mediaId, String userId, boolean isAdmin) {
        log.info("Deleting media with ID: {} by user: {} (admin: {})", mediaId, userId, isAdmin);

        PostMedia media = postMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        // Check permissions
        if (!isAdmin && !media.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own media files");
        }

        try {
            // Delete file from S3
            s3Helper.deleteFile(media.getMediaUrl());

            // Delete media record
            postMediaRepository.delete(media);
            log.info("Media deleted successfully with ID: {}", mediaId);
        } catch (Exception e) {
            log.error("Error deleting media: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete media: " + e.getMessage());
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