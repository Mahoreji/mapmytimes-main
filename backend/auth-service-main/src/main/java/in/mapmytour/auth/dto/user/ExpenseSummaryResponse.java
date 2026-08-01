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
public class ExpenseSummaryResponse {
    private String expenseId;
    private String description;
    private BigDecimal amount;
    private String paidBy;
    private String status;
}

