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
public class BalanceSummaryResponse {
    private String userId;
    private String userEmail;
    private String userName;
    private BigDecimal totalOwed; // Total amount this user owes
    private BigDecimal totalPaid; // Total amount this user has paid
    private BigDecimal netBalance; // Positive = owes money, Negative = is owed money
}

