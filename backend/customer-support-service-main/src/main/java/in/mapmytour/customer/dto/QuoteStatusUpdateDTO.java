package in.mapmytour.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteStatusUpdateDTO {
    @NotBlank
    private String status;

    private String assignedAgentId;
    private String notes;
}
