package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupExpenseRequest {
    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String category; // FOOD, ACCOMMODATION, TRANSPORT, ACTIVITY, OTHER

    @NotNull(message = "Expense date is required")
    private LocalDateTime expenseDate;

    private String receiptUrl;

    private String notes;

    @NotNull(message = "Participants are required")
    private List<String> participantUserIds; // List of user IDs who should split this expense
}

