package in.mapmytour.blog.dto.request.blogpost;

import com.fasterxml.jackson.databind.JsonNode;
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
public class UpdateBlogPostRequest {

    private String title;
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
    private String postType;

    // Denormalized Author Metadata
    private String authorEmail;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatarUrl;

    private List<String> categories;
    private List<String> tags;
    private String sectionSlug;
    private Boolean allowComments;
    private Boolean allowLikes;
    // Legacy support - flat structure (one subtitle per image)
    private List<MultipartFile> newMediaFiles;
    private List<String> newMediaCaptions; // Keep for backward compatibility
    private List<String> newMediaDescriptions; // Longer descriptions for new images
    private List<String> newMediaSubtitles; // Subtitles for new images
    
    // New grouped structure - multiple images per subtitle
    /**
     * Media groups where each group has a subtitle and multiple images.
     * Each group's images share the same subtitle but have individual descriptions.
     */
    private List<MediaGroupRequest> newMediaGroups;
    
    /**
     * All new media files in order for grouped structure.
     * The order should match the groups: first group's images, then second group's images, etc.
     */
    private List<MultipartFile> newGroupedMediaFiles;
    
    private List<String> mediaIdsToDelete;

    private List<String> mediaSubtitlesToDelete;

    private Boolean deleteAllMedia;

    private Boolean appendMedia;
}
