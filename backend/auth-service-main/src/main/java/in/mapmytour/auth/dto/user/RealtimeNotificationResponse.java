package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Unified real-time notification DTO for WebSocket delivery
 * Frontend subscribes to /user/queue/notifications to receive all notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeNotificationResponse {
    
    /**
     * Notification type: MESSAGE, CONNECTION_REQUEST, CONNECTION_ACCEPTED, 
     * CONNECTION_REJECTED, CONNECTION_WITHDRAWN, GROUP_MESSAGE, etc.
     */
    private String type;
    
    /**
     * Notification title
     */
    private String title;
    
    /**
     * Notification message/body
     */
    private String message;
    
    /**
     * Related entity ID (messageId, requestId, connectionId, etc.)
     */
    private String entityId;
    
    /**
     * Related entity type (MESSAGE, CONNECTION_REQUEST, etc.)
     */
    private String entityType;
    
    /**
     * Sender/actor user information
     */
    private NotificationUser sender;
    
    /**
     * Additional data payload (can contain full message, request details, etc.)
     */
    private Map<String, Object> data;
    
    /**
     * Timestamp when notification was created
     */
    private LocalDateTime timestamp;
    
    /**
     * Whether notification requires user action
     */
    private Boolean requiresAction;
    
    /**
     * Action URL or route (for frontend navigation)
     */
    private String actionUrl;
    
    /**
     * Notification priority: LOW, MEDIUM, HIGH, URGENT
     */
    private String priority;
    
    /**
     * User information for notifications
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationUser {
        private String userId;
        private String email;
        private String firstName;
        private String lastName;
        private String avatarUrl;
    }
}

