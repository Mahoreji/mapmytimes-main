package in.mapmytour.blog.dto.response.postcomment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCommentResponse {

    private String id;
    private String postId;
    private String content;
    private String userId;
    private String authorEmail;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatarUrl;
    private String parentCommentId;
    private String status;
    private List<PostCommentResponse> replies;
    private LocalDateTime createdAt;
}
