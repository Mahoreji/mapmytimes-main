package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request from booking-service to record an attribution.
 */
@Data
public class BookingAttributionRequest {

    @NotBlank
    private String bookingId;

    @NotBlank
    private String bookerUserId;

    private String circleId;

    private String postId;

    private String refUserId;

    @NotNull
    private BigDecimal amount;

    private String trackingToken;
}
