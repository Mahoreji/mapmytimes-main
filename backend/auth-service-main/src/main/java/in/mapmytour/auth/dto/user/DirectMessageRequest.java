package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectMessageRequest {
    @NotBlank(message = "Recipient email is required")
    private String recipientEmail;

    @NotBlank(message = "Message is required")
    private String message;

    private String messageType; // TEXT, IMAGE, LOCATION, FILE

    private String attachmentUrl;

    private String locationData;
}

