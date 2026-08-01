package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLockResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingAction {
        private String type; // e.g. CREATE_BOOKING, VIEW_DEALS, etc.
        private String label;
        private String url;
    }

    private String circleId;
    private List<BookingAction> actions;
    private String trackingToken;
}
