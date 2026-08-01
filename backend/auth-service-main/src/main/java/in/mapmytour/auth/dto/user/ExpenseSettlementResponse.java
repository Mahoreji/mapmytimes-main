package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSettlementResponse {
    private String groupId;
    private String groupName;
    private BigDecimal totalExpenses;
    private BigDecimal totalSettled;
    private BigDecimal totalPending;
    private List<ExpenseSummaryResponse> expenseSummary;
    private List<BalanceSummaryResponse> balances; // Who owes whom
}

