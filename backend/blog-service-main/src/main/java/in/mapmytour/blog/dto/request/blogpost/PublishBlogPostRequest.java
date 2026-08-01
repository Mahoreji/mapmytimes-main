package in.mapmytour.blog.dto.request.blogpost;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishBlogPostRequest {

    @NotBlank(message = "Post ID is required")
    private String postId;

    @NotBlank(message = "User ID is required")
    private String userId;
}
