package in.mapmytour.blog.dto.response.postlike;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLikeResponse {

    private String id;
    private String postId;
    private String userId;
    private String authorEmail;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatarUrl;
    private LocalDateTime likedAt;
}
