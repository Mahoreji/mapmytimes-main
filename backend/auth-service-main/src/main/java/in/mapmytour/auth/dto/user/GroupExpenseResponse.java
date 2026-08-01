package in.mapmytour.auth.dto.user;

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
public class GroupExpenseResponse {
    private String id;
    private String groupId;
    private String paidById;
    private String paidByEmail;
    private String paidByName;
    private String description;
    private BigDecimal amount;
    private String category;
    private LocalDateTime expenseDate;
    private String status;
    private String receiptUrl;
    private String notes;
    private List<ExpenseParticipantResponse> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

