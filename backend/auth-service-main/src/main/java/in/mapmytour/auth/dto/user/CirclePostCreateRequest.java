package in.mapmytour.auth.dto.user;

import in.mapmytour.auth.entity.PostType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Generic request for creating a circle post.
 */
@Data
public class CirclePostCreateRequest {

    @NotNull
    private PostType postType; // typically TEXT_UPDATE or PHOTO_UPDATE

    private String content;

    private String mediaUrl;

    private Double geoLat;

    private Double geoLng;
}
