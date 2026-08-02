package in.mapmytour.blog.dto.request.blogpost;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBlogPostRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private String slug;

    private String excerpt;

    private Integer readingTime;
    private JsonNode featuredImage;
    private String primaryVideoUrl;
    private JsonNode contentBlocks;
    private JsonNode tableOfContents;
    private JsonNode travelMeta;
    private JsonNode seo;
    private String visibility;
    private String language;
    private Boolean isFeatured;
    private Boolean isTrending;
    private LocalDateTime scheduledAt;
    private String postType; // BLOG, ARTICLE, VIDEO, SHORT, STORY, SOCIAL, PAGE

    private String userId;

    private String authorEmail;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatarUrl;

    private List<String> categories;
    private List<String> tags;
    private String sectionSlug;

    @Builder.Default
    private Boolean allowComments = true;

    @Builder.Default
    private Boolean allowLikes = true;

    // Legacy support - flat structure (one subtitle per image)
    private List<MultipartFile> mediaFiles;
    private List<String> mediaCaptions; // Keep for backward compatibility
    private List<String> mediaDescriptions; // Longer descriptions for each image
    private List<String> mediaSubtitles; // Subtitles for each image
    
    // New grouped structure - multiple images per subtitle
    /**
     * Media groups where each group has a subtitle and multiple images.
     * Each group's images share the same subtitle but have individual descriptions.
     * Example: 
     * - Group 1: subtitle="Day 1", images=[img1, img2, img3], descriptions=[desc1, desc2, desc3]
     * - Group 2: subtitle="Day 2", images=[img4, img5], descriptions=[desc4, desc5]
     */
    private List<MediaGroupRequest> mediaGroups;
    
    /**
     * All media files in order. The order should match the groups:
     * - First group's images, then second group's images, etc.
     * Example: [img1, img2, img3, img4, img5] for 2 groups (3+2 images)
     */
    private List<MultipartFile> groupedMediaFiles;
}
