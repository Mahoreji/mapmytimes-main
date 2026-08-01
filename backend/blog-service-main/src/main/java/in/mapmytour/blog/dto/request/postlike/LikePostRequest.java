package in.mapmytour.blog.dto.request.postlike;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikePostRequest {

    @NotBlank(message = "Post ID is required")
    private String postId;

    @NotBlank(message = "User ID is required")
    private String userId;
}
