package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionHistoryResponse {
    private String subscriptionId;
    private String plan;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double amount;
    private String currency;
    private String transactionId;
}
