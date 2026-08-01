package in.mapmytour.blog.dto.request.postmedia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadMediaRequest {

    @NotBlank(message = "Post ID is required")
    private String postId;

    @NotNull(message = "Media file is required")
    private MultipartFile mediaFile;

    private String caption; // Keep for backward compatibility
    private String description; // Longer description for the image
    private String subtitle; // Subtitle for the image

    @NotBlank(message = "User ID is required")
    private String userId;

    private Integer displayOrder;
}
