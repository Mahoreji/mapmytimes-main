package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Typing indicator for direct messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorResponse {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private boolean isTyping;
    private String conversationId; // recipientId for direct messages
}

