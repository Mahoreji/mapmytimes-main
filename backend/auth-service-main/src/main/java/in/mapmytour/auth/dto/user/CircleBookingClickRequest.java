package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request to record the last booking-related click inside a circle.
 */
@Data
public class CircleBookingClickRequest {

    @NotBlank
    private String circleId;

    private String postId;

    private String refUserId; // user who should receive attribution, typically post author or circle owner
}
