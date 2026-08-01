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
public class NotificationResponse {
    private String notificationId;
    private String type;
    private String channel;
    private String title;
    private String message;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private boolean isRead;
    private NotificationItemResponse.NotificationData data;
    private String errorMessage;
}