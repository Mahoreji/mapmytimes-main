package in.mapmytour.blog.dto.response.postmedia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMediaResponse {

    private String id;
    private String postId;
    private String mediaUrl;
    private String mediaType;
    private String caption; // Keep for backward compatibility
    private String description; // Longer description for the image
    private String subtitle; // Subtitle for the image group
    private Integer subtitleGroupIndex; // Groups images that share the same subtitle
    private String userId;
    private Integer displayOrder;
    private LocalDateTime uploadedAt;
}
