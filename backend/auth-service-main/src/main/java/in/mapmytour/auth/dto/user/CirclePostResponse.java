package in.mapmytour.auth.dto.user;

import in.mapmytour.auth.entity.PostType;
import in.mapmytour.auth.entity.CirclePostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for a post inside a trip circle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CirclePostResponse {

    private String id;
    private String circleId;
    private String authorUserId;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatarUrl;
    private boolean authorVerified;

    private PostType postType;
    private String content;
    private String mediaUrl;
    private Double geoLat;
    private Double geoLng;
    private CirclePostStatus status;
    private OffsetDateTime createdAt;
}
