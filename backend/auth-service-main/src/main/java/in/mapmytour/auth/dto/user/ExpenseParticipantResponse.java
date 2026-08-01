package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseParticipantResponse {
    private String id;
    private String userId;
    private String userEmail;
    private String userName;
    private BigDecimal shareAmount;
    private BigDecimal paidAmount;
    private BigDecimal balance; // Remaining balance (shareAmount - paidAmount)
    private String status;
    private LocalDateTime createdAt;
}

