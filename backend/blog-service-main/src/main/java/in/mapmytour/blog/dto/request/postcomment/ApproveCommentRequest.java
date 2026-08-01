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
public class ApproveCommentRequest {

    @NotBlank(message = "Comment ID is required")
    private String commentId;

    @NotBlank(message = "Action is required")
    private String action; // APPROVE, REJECT
}
