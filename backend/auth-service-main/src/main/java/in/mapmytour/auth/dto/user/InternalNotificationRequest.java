package in.mapmytour.auth.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalNotificationRequest {
    private String recipientUserId;
    private String senderUserId;
    private String type;
    private String message;
    private String postId;
    private String bookingId;
    private String paymentId;
    private String actionUrl;

    // Booking enrichment
    private String bookingType;
    private String serviceName;
    private String travelDate;
    private String source;
    private String destination;
    private String pnr;
    private String voucherUrl;

    // Payment enrichment
    private String amount;
    private String currency;
    private String paymentMethod;
    private String paymentStatus;
}
