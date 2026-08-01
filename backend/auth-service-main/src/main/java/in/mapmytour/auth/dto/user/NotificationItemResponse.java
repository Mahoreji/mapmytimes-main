package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemResponse {
    private String id;
    private String type; // SOCIAL_LIKE, SOCIAL_COMMENT, CONNECTION_REQUEST
    private String message;
    private LocalDateTime createdAt;
    private String actionUrl; // For frontend navigation
    private NotificationData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationData {
        private String postId;
        private String groupId;
        private String messageId;
        private String bookingId;
        private String paymentId;
        private String content; // Message preview or full content
        private String userName;
        private String userAvatar;
        private String actionUserId;
    }
}
