package in.mapmytour.auth.helper;

import in.mapmytour.auth.dto.user.NotificationItemResponse;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for sending real-time notifications via WebSocket
 * All notifications are automatically sent when REST endpoints are called
 * Frontend subscribes to /user/queue/notifications to receive all notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeNotificationHelper {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    /**
     * Send a real-time notification to a user
     * @param userEmail Recipient's email
     * @param type Notification type (MESSAGE, CONNECTION_REQUEST, etc.)
     * @param title Notification title
     * @param message Notification message
     * @param entityId Related entity ID
     * @param entityType Related entity type
     * @param sender Sender user information
     * @param data Additional data payload
     * @param requiresAction Whether notification requires user action
     * @param actionUrl Action URL for frontend navigation
     * @param priority Notification priority (LOW, MEDIUM, HIGH, URGENT)
     */
    public void sendNotification(String userEmail, String type, String title, String message,
                                String entityId, String entityType, User sender,
                                Map<String, Object> data, Boolean requiresAction,
                                String actionUrl, String priority) {
        try {
            NotificationItemResponse.NotificationData notificationData = NotificationItemResponse.NotificationData.builder()
                    .userName(sender != null ? sender.getFirstName() + " " + sender.getLastName() : null)
                    .userAvatar(sender != null ? sender.getAvatarUrl() : null)
                    .actionUserId(sender != null ? sender.getId() : null)
                    .build();

            // Extract specific IDs from extra data if present
            if (data != null) {
                if (data.containsKey("postId")) notificationData.setPostId((String) data.get("postId"));
                if (data.containsKey("groupId")) notificationData.setGroupId((String) data.get("groupId"));
                if (data.containsKey("messageId")) notificationData.setMessageId((String) data.get("messageId"));
                if (data.containsKey("content")) {
                    notificationData.setContent((String) data.get("content"));
                } else if (data.containsKey("message")) {
                    notificationData.setContent((String) data.get("message"));
                }
            }

            // Fallback for ID mapping if data map doesn't have them but type is known
            if (notificationData.getMessageId() == null && ("MESSAGE".equals(type) || "GROUP_MESSAGE".equals(type))) {
                notificationData.setMessageId(entityId);
            }
            if (notificationData.getPostId() == null && ("SOCIAL_LIKE".equals(type) || "SOCIAL_COMMENT".equals(type))) {
                notificationData.setPostId(entityId);
            }

            NotificationItemResponse notification = NotificationItemResponse.builder()
                    .id(entityId)
                    .type(type)
                    .message(title != null ? title : message)
                    .createdAt(LocalDateTime.now())
                    .actionUrl(actionUrl)
                    .data(notificationData)
                    .build();

            sendUnifiedNotification(userEmail, notification);

            log.debug("Sent unified real-time notification to {}: type={}, message={}", userEmail, type, message);
        } catch (Exception e) {
            log.warn("Failed to send real-time notification to {}: {}", userEmail, e.getMessage());
        }
    }

    /**
     * Send a message notification
     */
    public void sendMessageNotification(String recipientEmail, User sender, String messageId, 
                                       Map<String, Object> messageData) {
        sendNotification(
                recipientEmail,
                "MESSAGE",
                "New message from " + sender.getFirstName(),
                messageData != null && messageData.containsKey("message") 
                    ? (String) messageData.get("message") 
                    : "You have a new message",
                messageId,
                "MESSAGE",
                sender,
                messageData,
                true,
                "/messages/" + sender.getId(),
                "HIGH"
        );
    }

    /**
     * Send a connection request notification using unified schema
     */
    public void sendConnectionRequestNotification(String recipientEmail, User requester, String requestId) {
        NotificationItemResponse unifiedNotification = NotificationItemResponse.builder()
                .id(requestId)
                .type("CONNECTION_REQUEST")
                .message(requester.getFirstName() + " " + requester.getLastName() + " sent you a connection request")
                .createdAt(LocalDateTime.now())
                .data(NotificationItemResponse.NotificationData.builder()
                        .actionUserId(requester.getId())
                        .userName(requester.getFirstName() + " " + requester.getLastName())
                        .userAvatar(requester.getAvatarUrl())
                        .build())
                .build();

        sendUnifiedNotification(recipientEmail, unifiedNotification);
    }

    /**
     * Send a connection accepted notification
     */
    public void sendConnectionAcceptedNotification(String userEmail, User acceptedBy, String connectionId) {
        NotificationItemResponse unifiedNotification = NotificationItemResponse.builder()
                .id(connectionId)
                .type("CONNECTION_ACCEPTED")
                .message(acceptedBy.getFirstName() + " " + acceptedBy.getLastName() + " accepted your connection request")
                .createdAt(LocalDateTime.now())
                .actionUrl("/connections")
                .data(NotificationItemResponse.NotificationData.builder()
                        .actionUserId(acceptedBy.getId())
                        .userName(acceptedBy.getFirstName() + " " + acceptedBy.getLastName())
                        .userAvatar(acceptedBy.getAvatarUrl())
                        .build())
                .build();

        sendUnifiedNotification(userEmail, unifiedNotification);
    }

    /**
     * Send a connection request withdrawn notification
     */
    public void sendConnectionWithdrawnNotification(String recipientEmail, User requester, String requestId) {
        NotificationItemResponse unifiedNotification = NotificationItemResponse.builder()
                .id(requestId)
                .type("CONNECTION_WITHDRAWN")
                .message(requester.getFirstName() + " " + requester.getLastName() + " withdrew their connection request")
                .createdAt(LocalDateTime.now())
                .data(NotificationItemResponse.NotificationData.builder()
                        .actionUserId(requester.getId())
                        .userName(requester.getFirstName() + " " + requester.getLastName())
                        .userAvatar(requester.getAvatarUrl())
                        .build())
                .build();

        sendUnifiedNotification(recipientEmail, unifiedNotification);
    }

    /**
     * Send a group message notification
     */
    public void sendGroupMessageNotification(String userEmail, User sender, String groupId, 
                                            String groupName, String messageId, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        data.put("groupName", groupName);
        data.put("messageId", messageId);
        data.put("message", message);

        sendNotification(
                userEmail,
                "GROUP_MESSAGE",
                "New message in " + (groupName != null ? groupName : "group"),
                sender.getFirstName() + ": " + (message != null && message.length() > 50 
                    ? message.substring(0, 50) + "..." 
                    : message),
                messageId,
                "GROUP_MESSAGE",
                sender,
                data,
                true,
                "/groups/" + groupId,
                "MEDIUM"
        );
    }

    /**
     * Send a unified notification matching the frontend's expected JSON structure
     */
    public void sendUnifiedNotification(String userEmail, NotificationItemResponse notification) {
        try {
            // First send real-time
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/notifications",
                    notification
            );
            
            // Then record for history
            notificationService.recordNotification(userEmail, notification);
            
            log.debug("Sent and recorded unified notification to {}: type={}", userEmail, notification.getType());
        } catch (Exception e) {
            log.warn("Failed to process unified notification to {}: {}", userEmail, e.getMessage());
        }
    }
}
