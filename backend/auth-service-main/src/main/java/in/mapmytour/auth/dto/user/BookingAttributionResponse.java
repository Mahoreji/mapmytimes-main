package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAttributionResponse {

    private String bookingId;
    private String bookerUserId;
    private String circleId;
    private String postId;
    private String refUserId;
    private boolean created;
    private boolean eligible;
    private BigDecimal amount;
}
