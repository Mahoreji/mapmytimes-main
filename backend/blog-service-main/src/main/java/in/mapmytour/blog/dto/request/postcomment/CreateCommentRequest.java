package in.mapmytour.blog.dto.request.postcomment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCommentRequest {

    @NotBlank(message = "Post ID is required")
    private String postId;

    @NotBlank(message = "Comment content is required")
    private String content;

    private String userId;

    private String parentCommentId;
}
