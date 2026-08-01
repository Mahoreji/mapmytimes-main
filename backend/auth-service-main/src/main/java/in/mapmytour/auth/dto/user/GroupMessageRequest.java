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
public class GroupMessageRequest {
    @NotBlank(message = "Message is required")
    private String message;

    private String messageType; // TEXT, IMAGE, LOCATION, FILE, SYSTEM

    private String attachmentUrl; // For images, files

    private String locationData; // JSON string for location sharing
}

