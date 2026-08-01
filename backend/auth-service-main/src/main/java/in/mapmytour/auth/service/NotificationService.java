package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.user.NotificationRequest;
import in.mapmytour.auth.dto.user.NotificationItemResponse;
import in.mapmytour.auth.dto.user.NotificationResponse;
import in.mapmytour.auth.entity.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    // Email notifications
    CompletableFuture<Boolean> sendEmail(String to, String subject, String content, String templateName, Map<String, Object> variables);
    CompletableFuture<Boolean> sendBulkEmail(List<String> recipients, String subject, String content, String templateName, Map<String, Object> variables);

    // SMS notifications
    CompletableFuture<Boolean> sendSMS(String phoneNumber, String message);
    CompletableFuture<Boolean> sendBulkSMS(List<String> phoneNumbers, String message);

    // Push notifications
    CompletableFuture<Boolean> sendPushNotification(String deviceToken, String title, String message, Map<String, Object> data);
    CompletableFuture<Boolean> sendBulkPushNotification(List<String> deviceTokens, String title, String message, Map<String, Object> data);

    // User-specific notifications
    CompletableFuture<Boolean> sendNotificationToUser(String userEmail, NotificationRequest request);
    CompletableFuture<Boolean> sendNotificationToUsers(List<String> userEmails, NotificationRequest request);

    // Template-based notifications
    CompletableFuture<Boolean> sendWelcomeNotification(User user);
    CompletableFuture<Boolean> sendVerificationNotification(User user, String verificationCode);
    CompletableFuture<Boolean> sendPasswordResetNotification(User user, String resetToken);
    CompletableFuture<Boolean> sendLoginAlertNotification(User user, String ipAddress, String userAgent);
    CompletableFuture<Boolean> sendSecurityAlertNotification(User user, String alertType, String description);

    // Subscription and marketing
    CompletableFuture<Boolean> sendMarketingNotification(User user, String campaignId, Map<String, Object> personalizedData);
    CompletableFuture<Boolean> sendSubscriptionNotification(User user, String subscriptionEvent, Map<String, Object> subscriptionData);

    // System notifications
    CompletableFuture<Boolean> sendSystemMaintenanceNotification(List<String> userEmails, String maintenanceInfo);
    CompletableFuture<Boolean> sendSystemUpdateNotification(List<String> userEmails, String updateInfo);

    // Notification preferences
    boolean isNotificationAllowed(String userEmail, String notificationType, String channel);
    void updateNotificationPreferences(String userEmail, Map<String, Boolean> preferences);

    // Notification history and tracking
    List<NotificationResponse> getNotificationHistory(String userEmail, int page, int size);
    boolean markNotificationAsRead(String userEmail, String notificationId);
    boolean markAllNotificationsAsRead(String userEmail);
    void recordNotification(String userEmail, NotificationItemResponse notification);

    // Notification templates
    String processTemplate(String templateName, Map<String, Object> variables);
    boolean createNotificationTemplate(String templateName, String subject, String content, String channel);
}