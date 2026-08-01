package in.mapmytour.blog.dto.request.postmedia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMediaRequest {

    private String caption; // Keep for backward compatibility
    private String description; // Longer description for the image
    private String subtitle; // Subtitle for the image
    private Integer displayOrder;
    private MultipartFile newMediaFile;
}
