package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper for booking lock actions and tracking token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleBookingLocksResponse {
    private String circleId;
    private List<BookingLockResponse.BookingAction> actions;
    private String trackingToken;
}
