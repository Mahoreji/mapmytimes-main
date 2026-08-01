package in.mapmytour.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotBlank(message = "Ticket ID is required")
    private String ticketId;

    @NotBlank(message = "Sender ID is required")
    private String senderId;

    @NotBlank(message = "Message is required")
    private String message;

    private String attachmentUrl;
    private boolean isInternalNote;
}