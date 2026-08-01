package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectMessageResponse {
    private String id;
    private String senderId;
    private String senderEmail;
    private String senderName;
    private String senderAvatarUrl;
    private String recipientId;
    private String recipientEmail;
    private String recipientName;
    private String recipientAvatarUrl;
    private String message;
    private String messageType;
    private String attachmentUrl;
    private String locationData;
    private String status;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private boolean isFromMe; // Helper field to identify if message is from current user
}

