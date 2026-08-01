package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Real-time message status update (SENT, DELIVERED, READ)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusUpdateResponse {
    private String messageId;
    private String conversationId; // recipientId for direct, groupId for group
    private String status; // SENT, DELIVERED, READ
    private LocalDateTime readAt;
    private String updatedBy; // userId who read the message
}

