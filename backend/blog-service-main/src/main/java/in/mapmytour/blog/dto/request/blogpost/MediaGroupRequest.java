package in.mapmytour.blog.dto.request.blogpost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a group of media files that share the same subtitle.
 * Multiple images can belong to the same subtitle group.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaGroupRequest {
    
    /**
     * Subtitle for this group of images
     */
    private String subtitle;
    
    /**
     * List of descriptions, one for each image in this group.
     * The index corresponds to the image file index in mediaFiles.
     */
    private List<String> descriptions;
    
    /**
     * List of captions (optional, for backward compatibility)
     */
    private List<String> captions;
}

