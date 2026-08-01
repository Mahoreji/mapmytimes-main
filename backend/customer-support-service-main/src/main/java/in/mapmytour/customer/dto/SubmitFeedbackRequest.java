package in.mapmytour.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitFeedbackRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    private String ticketId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String comments;
    private boolean isFollowUpRequired;
}