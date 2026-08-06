package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.user.NotificationRequest;
import in.mapmytour.auth.dto.user.NotificationItemResponse;
import in.mapmytour.auth.dto.user.NotificationResponse;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.dto.notification.SendNotificationRequest;
import in.mapmytour.auth.dto.notification.SendNotificationResponse;
import in.mapmytour.auth.entity.Notification;
import in.mapmytour.auth.repository.NotificationRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.service.NotificationService;
import in.mapmytour.auth.client.NotificationServiceClient;
import in.mapmytour.auth.utils.SignatureUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationServiceClient notificationServiceClient;
    private final SignatureUtils signatureUtils;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    // Notification templates
    private final Map<String, String> notificationTemplates = new HashMap<>();

    // ================ EMAIL NOTIFICATIONS ================

    @Override
    public CompletableFuture<Boolean> sendEmail(String to, String subject, String content, String templateName, Map<String, Object> variables) {
        try {
            String type = "email_html";
            Map<String, Object> metadata = new HashMap<>();
            String body = content;

            if (templateName != null) {
                type = "email_template";
                metadata.put("template_name", templateName.endsWith(".html") ? templateName : templateName + ".html");
                
                if (variables != null) {
                    metadata.putAll(variables);
                    if (variables.containsKey("firstName")) {
                        metadata.put("name", variables.get("firstName"));
                    }
                    if (variables.containsKey("verificationCode")) {
                        body = String.valueOf(variables.get("verificationCode"));
                    } else if (variables.containsKey("resetToken")) {
                        body = String.valueOf(variables.get("resetToken"));
                    }
                }

                if (body == null || body.trim().isEmpty()) {
                    body = "Templated Email: " + templateName;
                }
            }

            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipient(to)
                    .subject(subject)
                    .body(body)
                    .type(type)
                    .source("auth-service")
                    .metadata(metadata)
                    .build();

            Map<String, String> headersToSign = new HashMap<>();
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            headersToSign.put("X-Request-Source", "internal-service");
            headersToSign.put("X-Gateway-Timestamp", timestamp);
            String signature = signatureUtils.generateSignature(headersToSign);

            SendNotificationResponse response = notificationServiceClient.sendInstantNotification(request, "internal-service", timestamp, signature);
            boolean success = response != null && "processing".equals(response.getStatus());

            recordNotification(to, "EMAIL", subject, body, success ? "SENT" : "FAILED");
            return CompletableFuture.completedFuture(success);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            recordNotification(to, "EMAIL", subject, content, "FAILED");
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> sendBulkEmail(List<String> recipients, String subject, String content, String templateName, Map<String, Object> variables) {
        boolean allSuccess = true;
        for (String recipient : recipients) {
            if (!sendEmail(recipient, subject, content, templateName, variables).join()) {
                allSuccess = false;
            }

            // Add small delay to prevent overwhelming email service
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return CompletableFuture.completedFuture(allSuccess);
    }

    // ================ SMS NOTIFICATIONS ================

    @Override
    public CompletableFuture<Boolean> sendSMS(String phoneNumber, String message) {
        try {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipient(phoneNumber)
                    .body(message)
                    .type("sms")
                    .source("auth-service")
                    .metadata(new HashMap<>())
                    .build();

            Map<String, String> headersToSign = new HashMap<>();
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            headersToSign.put("X-Request-Source", "internal-service");
            headersToSign.put("X-Gateway-Timestamp", timestamp);
            String signature = signatureUtils.generateSignature(headersToSign);

            SendNotificationResponse response = notificationServiceClient.sendInstantNotification(request, "internal-service", timestamp, signature);
            boolean success = response != null && "processing".equals(response.getStatus());

            recordNotification(phoneNumber, "SMS", "SMS", message, success ? "SENT" : "FAILED");
            return CompletableFuture.completedFuture(success);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            recordNotification(phoneNumber, "SMS", "SMS", message, "FAILED");
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> sendBulkSMS(List<String> phoneNumbers, String message) {
        boolean allSuccess = true;
        for (String phoneNumber : phoneNumbers) {
            if (!sendSMS(phoneNumber, message).join()) {
                allSuccess = false;
            }
        }
        return CompletableFuture.completedFuture(allSuccess);
    }

    // ================ PUSH NOTIFICATIONS ================

    @Override
    public CompletableFuture<Boolean> sendPushNotification(String deviceToken, String title, String message, Map<String, Object> data) {
        // Implementation for FCM/APNS push notifications
        // This would integrate with Firebase Cloud Messaging or Apple Push Notification service
        log.info("Sending push notification to device: {} - Title: {}, Message: {}", deviceToken, title, message);

        recordNotification(deviceToken, "PUSH", title, message, "SENT");
        return CompletableFuture.completedFuture(true); // Placeholder implementation
    }

    @Override
    public CompletableFuture<Boolean> sendBulkPushNotification(List<String> deviceTokens, String title, String message, Map<String, Object> data) {
        boolean allSuccess = true;
        for (String deviceToken : deviceTokens) {
            if (!sendPushNotification(deviceToken, title, message, data).join()) {
                allSuccess = false;
            }
        }
        return CompletableFuture.completedFuture(allSuccess);
    }

    // ================ USER-SPECIFIC NOTIFICATIONS ================

    @Override
    public CompletableFuture<Boolean> sendNotificationToUser(String userEmail, NotificationRequest request) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            boolean success = false;

            switch (request.getChannel().toUpperCase()) {
                case "EMAIL":
                    if (isNotificationAllowed(userEmail, request.getType(), "EMAIL")) {
                        success = sendEmail(userEmail, request.getTitle(), request.getMessage(),
                                request.getTemplateName(), request.getTemplateVariables()).join();
                    }
                    break;

                case "SMS":
                    if (user.getPhone() != null && isNotificationAllowed(userEmail, request.getType(), "SMS")) {
                        success = sendSMS(user.getPhone(), request.getMessage()).join();
                    }
                    break;

                case "PUSH":
                    if (isNotificationAllowed(userEmail, request.getType(), "PUSH")) {
                        // Get user's device tokens and send push notification
                        success = sendPushNotification("device_token_placeholder", request.getTitle(),
                                request.getMessage(), request.getMetadata()).join();
                    }
                    break;

                case "ALL":
                    boolean emailSuccess = false, smsSuccess = false, pushSuccess = false;

                    if (isNotificationAllowed(userEmail, request.getType(), "EMAIL")) {
                        emailSuccess = sendEmail(userEmail, request.getTitle(), request.getMessage(),
                                request.getTemplateName(), request.getTemplateVariables()).join();
                    }

                    if (user.getPhone() != null && isNotificationAllowed(userEmail, request.getType(), "SMS")) {
                        smsSuccess = sendSMS(user.getPhone(), request.getMessage()).join();
                    }

                    if (isNotificationAllowed(userEmail, request.getType(), "PUSH")) {
                        pushSuccess = sendPushNotification("device_token_placeholder", request.getTitle(),
                                request.getMessage(), request.getMetadata()).join();
                    }

                    success = emailSuccess || smsSuccess || pushSuccess;
                    break;
            }

            return CompletableFuture.completedFuture(success);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> sendNotificationToUsers(List<String> userEmails, NotificationRequest request) {
        boolean allSuccess = true;
        for (String userEmail : userEmails) {
            if (!sendNotificationToUser(userEmail, request).join()) {
                allSuccess = false;
            }
        }
        return CompletableFuture.completedFuture(allSuccess);
    }

    // ================ TEMPLATE-BASED NOTIFICATIONS ================

    @Override
    public CompletableFuture<Boolean> sendWelcomeNotification(User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("email", user.getEmail());
        variables.put("appName", "MapMyTimes");

        return sendEmail(user.getEmail(), "Welcome to MapMyTimes!", null, "welcome_email", variables);
    }

    @Override
    public CompletableFuture<Boolean> sendVerificationNotification(User user, String verificationCode) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("verificationCode", verificationCode);
        variables.put("appName", "MapMyTimes");

        boolean emailSent = sendEmail(user.getEmail(), "Email Verification - MapMyTimes", null, "verification_email", variables).join();

        // Also send SMS if phone number is available
        boolean smsSent = false;
        if (user.getPhone() != null) {
            String smsMessage = String.format("Your MapMyTimes verification code is: %s. Valid for 10 minutes.", verificationCode);
            smsSent = sendSMS(user.getPhone(), smsMessage).join();
        }

        return CompletableFuture.completedFuture(emailSent || smsSent);
    }

    @Override
    public CompletableFuture<Boolean> sendPasswordResetNotification(User user, String resetToken) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("resetToken", resetToken);
        variables.put("appName", "MapMyTimes");

        return sendEmail(user.getEmail(), "Password Reset - MapMyTimes", null, "password_reset_email", variables);
    }

    @Override
    public CompletableFuture<Boolean> sendLoginAlertNotification(User user, String ipAddress, String userAgent) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("ipAddress", ipAddress);
        variables.put("userAgent", userAgent);
        variables.put("loginTime", LocalDateTime.now().toString());
        variables.put("appName", "MapMyTimes");

        return sendEmail(user.getEmail(), "New Login Alert - MapMyTimes", null, "login_alert_email", variables);
    }

    @Override
    public CompletableFuture<Boolean> sendSecurityAlertNotification(User user, String alertType, String description) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("alertType", alertType);
        variables.put("description", description);
        variables.put("timestamp", LocalDateTime.now().toString());
        variables.put("appName", "MapMyTimes");

        return sendEmail(user.getEmail(), "Security Alert - MapMyTimes", null, "security_alert_email", variables);
    }

    // ================ SUBSCRIPTION AND MARKETING ================

    @Override
    public CompletableFuture<Boolean> sendMarketingNotification(User user, String campaignId, Map<String, Object> personalizedData) {
        if (!isNotificationAllowed(user.getEmail(), "MARKETING", "EMAIL")) {
            return CompletableFuture.completedFuture(false);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.putAll(personalizedData);
        variables.put("campaignId", campaignId);

        return sendEmail(user.getEmail(), "Special Offer from MapMyTimes", null, "marketing_email", variables);
    }

    @Override
    public CompletableFuture<Boolean> sendSubscriptionNotification(User user, String subscriptionEvent, Map<String, Object> subscriptionData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("subscriptionEvent", subscriptionEvent);
        variables.putAll(subscriptionData);

        String subject = getSubscriptionEmailSubject(subscriptionEvent);
        return sendEmail(user.getEmail(), subject, null, "subscription_email", variables);
    }

    // ================ SYSTEM NOTIFICATIONS ================

    @Override
    public CompletableFuture<Boolean> sendSystemMaintenanceNotification(List<String> userEmails, String maintenanceInfo) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("maintenanceInfo", maintenanceInfo);
        variables.put("appName", "MapMyTimes");

        return sendBulkEmail(userEmails, "Scheduled Maintenance - MapMyTimes", null, "maintenance_email", variables);
    }

    @Override
    public CompletableFuture<Boolean> sendSystemUpdateNotification(List<String> userEmails, String updateInfo) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("updateInfo", updateInfo);
        variables.put("appName", "MapMyTimes");

        return sendBulkEmail(userEmails, "System Update - MapMyTimes", null, "system_update_email", variables);
    }

    // ================ NOTIFICATION PREFERENCES ================

    @Override
    public boolean isNotificationAllowed(String userEmail, String notificationType, String channel) {
        try {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user == null || user.getPreferences() == null || user.getPreferences().getNotifications() == null) {
                return true; // Default to allowing notifications
            }

            User.NotificationPreferences prefs = user.getPreferences().getNotifications();

            return switch (channel.toUpperCase()) {
                case "EMAIL" -> prefs.getEmail();
                case "SMS" -> prefs.getSms();
                case "PUSH" -> prefs.getPush();
                default -> true;
            };
        } catch (Exception e) {
            log.error("Error checking notification preferences for user {}: {}", userEmail, e.getMessage());
            return true; // Default to allowing notifications on error
        }
    }

    @Override
    public void updateNotificationPreferences(String userEmail, Map<String, Boolean> preferences) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            User.UserPreferences userPrefs = user.getPreferences();
            if (userPrefs == null) {
                userPrefs = User.UserPreferences.builder().build();
            }

            User.NotificationPreferences notifPrefs = userPrefs.getNotifications();
            if (notifPrefs == null) {
                notifPrefs = User.NotificationPreferences.builder().build();
            }

            for (Map.Entry<String, Boolean> entry : preferences.entrySet()) {
                String key = entry.getKey().toLowerCase();
                Boolean value = entry.getValue();
                switch (key) {
                    case "email" -> notifPrefs.setEmail(value);
                    case "sms" -> notifPrefs.setSms(value);
                    case "push" -> notifPrefs.setPush(value);
                }
            }

            userPrefs.setNotifications(notifPrefs);
            user.setPreferences(userPrefs);
            userRepository.save(user);

        } catch (Exception e) {
            log.error("Error updating notification preferences for user {}: {}", userEmail, e.getMessage());
        }
    }

    // ================ NOTIFICATION HISTORY ================

    @Override
    public List<NotificationResponse> getNotificationHistory(String userEmail, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        List<Notification> notifications = notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(userEmail, pageable);
        
        List<NotificationResponse> history = new ArrayList<>();
        for (Notification n : notifications) {
            history.add(NotificationResponse.builder()
                .notificationId(n.getId())
                .type(n.getType())
                .channel(n.getChannel())
                .title(n.getMessage())
                .message(n.getMessage())
                .sentAt(n.getCreatedAt())
                .isRead(n.isRead())
                .readAt(n.getReadAt())
                .status(n.getStatus())
                .data(NotificationItemResponse.NotificationData.builder()
                        .postId(n.getPostId())
                        .bookingId(n.getBookingId())
                        .paymentId(n.getPaymentId())
                        .userName(n.getSenderName())
                        .userAvatar(n.getSenderAvatar())
                        .actionUserId(n.getSenderId())
                        .build())
                .build());
        }
        return history;
    }

    @Override
    public boolean markNotificationAsRead(String userEmail, String notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    if (notification.getRecipientEmail().equals(userEmail)) {
                        notification.setRead(true);
                        notification.setReadAt(LocalDateTime.now());
                        notificationRepository.save(notification);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    public boolean markAllNotificationsAsRead(String userEmail) {
        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);
            List<Notification> unread = notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(userEmail, pageable);
            unread.forEach(n -> {
                if (!n.isRead()) {
                    n.setRead(true);
                    n.setReadAt(LocalDateTime.now());
                }
            });
            notificationRepository.saveAll(unread);
            return true;
        } catch (Exception e) {
            log.error("Failed to mark all notifications as read for {}: {}", userEmail, e.getMessage());
            return false;
        }
    }

    @Override
    public void recordNotification(String userEmail, NotificationItemResponse notification) {
        Notification entity = Notification.builder()
                .recipientEmail(userEmail)
                .type(notification.getType())
                .channel("PUSH")
                .message(notification.getMessage() != null ? notification.getMessage() : "Notification")
                .actionUrl(notification.getActionUrl())
                .status("SENT")
                .isRead(false)
                .createdAt(notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now())
                .build();
        
        if (notification.getData() != null) {
            entity.setPostId(notification.getData().getPostId());
            entity.setBookingId(notification.getData().getBookingId());
            entity.setPaymentId(notification.getData().getPaymentId());
            entity.setSenderName(notification.getData().getUserName());
            entity.setSenderAvatar(notification.getData().getUserAvatar());
            entity.setSenderId(notification.getData().getActionUserId());
        }
        
        notificationRepository.save(entity);
    }

    // ================ TEMPLATE PROCESSING ================

    @Override
    public String processTemplate(String templateName, Map<String, Object> variables) {
        String template = notificationTemplates.get(templateName);
        if (template == null) {
            return getDefaultTemplate(templateName, variables);
        }

        String processed = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            processed = processed.replace(placeholder, String.valueOf(entry.getValue()));
        }

        return processed;
    }

    @Override
    public boolean createNotificationTemplate(String templateName, String subject, String content, String channel) {
        notificationTemplates.put(templateName, content);
        return true;
    }

    // ================ PRIVATE HELPER METHODS ================

    private void recordNotification(String recipient, String channel, String title, String message, String status) {
        Notification notification = Notification.builder()
                .recipientEmail(recipient)
                .channel(channel)
                .type(channel.equals("EMAIL") ? "SYSTEM_EMAIL" : "SYSTEM_NOTIFICATION")
                .message(message != null ? message : (title != null ? title : "No Message"))
                .status(status)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    private String getSubscriptionEmailSubject(String subscriptionEvent) {
        return switch (subscriptionEvent.toUpperCase()) {
            case "UPGRADE" -> "Subscription Upgraded - MapMyTimes";
            case "DOWNGRADE" -> "Subscription Changed - MapMyTimes";
            case "RENEWAL" -> "Subscription Renewed - MapMyTimes";
            case "CANCELLATION" -> "Subscription Cancelled - MapMyTimes";
            case "EXPIRY_WARNING" -> "Subscription Expiring Soon - MapMyTimes";
            default -> "Subscription Update - MapMyTimes";
        };
    }

    private String getDefaultTemplate(String templateName, Map<String, Object> variables) {
        // Return simple default templates
        return switch (templateName) {
            case "welcome_email" ->
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
                        <style>
                            body { margin: 0; padding: 0; background-color: #F8FAFC; font-family: 'Plus Jakarta Sans', 'Inter', Helvetica, Arial, sans-serif; }
                            .wrapper { width: 100%; table-layout: fixed; background-color: #F8FAFC; padding: 48px 0; }
                            .main { background-color: #FFFFFF; margin: 0 auto; width: 100%; max-width: 550px; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); }
                            .header { background-color: #FFFFFF; padding: 48px 40px; text-align: center; border-bottom: 1px solid #F1F5F9; }
                            .logo-img { height: 64px; width: auto; display: block; margin: 0 auto; }
                            .content { padding: 56px 48px; text-align: center; color: #1E293B; }
                            .title { font-size: 30px; font-weight: 800; color: #0F172A; margin-bottom: 20px; letter-spacing: -0.5px; }
                            .text { font-size: 16px; line-height: 1.7; color: #475569; margin-bottom: 32px; }
                            .cta-button { display: inline-block; padding: 18px 36px; background-color: #E96B5B; color: #FFFFFF; text-decoration: none; border-radius: 14px; font-weight: 700; margin-top: 24px; box-shadow: 0 10px 15px -3px rgba(233, 107, 91, 0.2); }
                            .signature { margin-top: 56px; text-align: left; border-top: 1px solid #F1F5F9; padding-top: 32px; }
                            .footer { padding: 40px; text-align: center; background-color: #F8FAFC; border-top: 1px solid #E2E8F0; }
                            .footer-brand { font-size: 13px; font-weight: 700; color: #64748B; margin-bottom: 12px; }
                            .disclaimer { font-size: 11px; color: #94A3B8; line-height: 1.6; }
                        </style>
                    </head>
                    <body>
                        <center class="wrapper">
                            <div class="main">
                                <div class="header">
                                    <img src="https://www.mapmytimes.com/logo.svg" alt="MapMyTimes" class="logo-img">
                                </div>
                                <div class="content">
                                    <h2 class="title">Welcome to the Journey!</h2>
                                    <p class="text">Hello <strong>{{firstName}}</strong>,<br>We are absolutely delighted to welcome you to MapMyTimes. You've joined a premier community of travelers dedicated to discovering the world's most exceptional experiences.</p>
                                    <p class="text">Your account is now active. Explore our curated destinations and start planning your next unforgettable adventure today.</p>
                                    <a href="#" class="cta-button">Start Exploring</a>
                                    
                                    <div class="signature">
                                        <p style="margin: 0; font-size: 15px; color: #64748B;">Warm Regards,</p>
                                        <p style="margin: 4px 0 0 0; font-size: 16px; font-weight: 700; color: #1E293B;">MapMyTimes Concierge</p>
                                    </div>
                                </div>
                                <div class="footer">
                                    <div class="footer-brand">© 2026 MapMyTimes. All rights reserved.</div>
                                    <p class="disclaimer">
                                        This email was sent to you as a registered member of MapMyTimes. <br>
                                        <strong>Confidentiality Note:</strong> This message is intended only for the use of the individual or entity to which it is addressed and may contain information that is privileged and confidential.
                                    </p>
                                </div>
                            </div>
                        </center>
                    </body>
                    </html>
                    """;

            case "verification_email" ->
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
                        <style>
                            body { margin: 0; padding: 0; background-color: #F1F5F9; font-family: 'Plus Jakarta Sans', 'Inter', Helvetica, Arial, sans-serif; }
                            .wrapper { width: 100%; table-layout: fixed; background-color: #F1F5F9; padding: 48px 0; }
                            .main { background-color: #FFFFFF; margin: 0 auto; width: 100%; max-width: 520px; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }
                            .header { background-color: #FFFFFF; padding: 40px; text-align: center; border-bottom: 1px solid #F1F5F9; }
                            .logo-img { height: 64px; width: auto; display: block; margin: 0 auto; }
                            .content { padding: 56px 40px; text-align: center; color: #1E293B; }
                            .title { font-size: 26px; font-weight: 800; color: #0F172A; margin-bottom: 16px; letter-spacing: -0.5px; }
                            .text { font-size: 15px; line-height: 1.6; color: #475569; margin-bottom: 32px; }
                            .otp-container { background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 32px 20px; margin: 24px 0; }
                            .otp-label { font-size: 11px; font-weight: 700; color: #94A3B8; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 16px; }
                            .otp-code { font-size: 38px; font-weight: 800; color: #1E293B; letter-spacing: 10px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; white-space: nowrap; display: block; }
                            .notice { font-size: 13px; color: #E96B5B; font-weight: 700; margin-top: 24px; }
                            .footer { padding: 32px 40px; text-align: center; background-color: #F8FAFC; border-top: 1px solid #F1F5F9; }
                            .footer-brand { font-size: 12px; font-weight: 700; color: #475569; margin-bottom: 12px; }
                            .disclaimer { font-size: 10px; color: #94A3B8; line-height: 1.5; }
                        </style>
                    </head>
                    <body>
                        <center class="wrapper">
                            <div class="main">
                                <div class="header">
                                    <img src="https://www.mapmytimes.com/logo.svg" alt="MapMyTimes" class="logo-img">
                                </div>
                                <div class="content">
                                    <h2 class="title">Security Verification</h2>
                                    <p class="text">Hello <strong>{{firstName}}</strong>,<br>Please use the following single-use verification code to authenticate your request. For your security, do not share this code with anyone.</p>
                                    
                                    <div class="otp-container">
                                        <div class="otp-label">Verification Code</div>
                                        <div class="otp-code">{{verificationCode}}</div>
                                    </div>
                                    
                                    <p class="notice">Valid for 10 minutes only</p>
                                    
                                    <div style="margin-top: 56px; text-align: left; border-top: 1px solid #F1F5F9; padding-top: 24px;">
                                        <p style="margin: 0; font-size: 14px; color: #64748B;">Best Regards,</p>
                                        <p style="margin: 4px 0 0 0; font-size: 15px; font-weight: 700; color: #1E293B;">MapMyTimes Security Team</p>
                                    </div>
                                </div>
                                <div class="footer">
                                    <div class="footer-brand">© 2026 MapMyTimes. All rights reserved.</div>
                                    <p class="disclaimer">
                                        <strong>Security Note:</strong> This is an automated notification. If you did not request this verification code, please contact our security center immediately.
                                    </p>
                                </div>
                            </div>
                        </center>
                    </body>
                    </html>
                    """;

            case "password_reset_email" ->
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
                        <style>
                            body { margin: 0; padding: 0; background-color: #F8FAFC; font-family: 'Plus Jakarta Sans', 'Inter', Helvetica, Arial, sans-serif; }
                            .wrapper { width: 100%; table-layout: fixed; background-color: #F8FAFC; padding: 48px 0; }
                            .main { background-color: #FFFFFF; margin: 0 auto; width: 100%; max-width: 550px; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); }
                            .header { background-color: #FFFFFF; padding: 48px 40px; text-align: center; border-bottom: 1px solid #F1F5F9; }
                            .logo-img { height: 64px; width: auto; display: block; margin: 0 auto; }
                            .content { padding: 56px 48px; text-align: center; color: #1E293B; }
                            .title { font-size: 28px; font-weight: 800; color: #0F172A; margin-bottom: 20px; letter-spacing: -0.5px; }
                            .text { font-size: 16px; line-height: 1.7; color: #475569; margin-bottom: 32px; }
                            .token-box { background-color: #F8FAFC; border-radius: 12px; padding: 24px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 20px; color: #1E293B; word-break: break-all; margin: 24px 0; border: 1px solid #E2E8F0; letter-spacing: 1px; }
                            .signature { margin-top: 56px; text-align: left; border-top: 1px solid #F1F5F9; padding-top: 32px; }
                            .footer { padding: 40px; text-align: center; background-color: #F8FAFC; border-top: 1px solid #E2E8F0; }
                            .footer-brand { font-size: 13px; font-weight: 700; color: #64748B; margin-bottom: 12px; }
                            .disclaimer { font-size: 10px; color: #94A3B8; line-height: 1.6; }
                        </style>
                    </head>
                    <body>
                        <center class="wrapper">
                            <div class="main">
                                <div class="header">
                                    <img src="https://www.mapmytimes.com/logo.svg" alt="MapMyTimes" class="logo-img">
                                </div>
                                <div class="content">
                                    <h2 class="title">Password Reset Request</h2>
                                    <p class="text">Hello {{firstName}},<br>We received a formal request to reset the password for your MapMyTimes account. To proceed with the reset, please use the following unique security token.</p>
                                    <p class="text">If you did not initiate this request, please ignore this email; your account remains secure.</p>
                                    
                                    <div class="token-box">{{resetToken}}</div>
                                    
                                    <div class="signature">
                                        <p style="margin: 0; font-size: 15px; color: #64748B;">Best Regards,</p>
                                        <p style="margin: 4px 0 0 0; font-size: 16px; font-weight: 700; color: #1E293B;">MapMyTimes Security Team</p>
                                    </div>
                                </div>
                                <div class="footer">
                                    <div class="footer-brand">© 2026 MapMyTimes. All rights reserved.</div>
                                    <p class="disclaimer">
                                        This is a system-generated security notification. <br>
                                        <strong>Confidentiality Notice:</strong> This email is intended solely for the person or entity to which it is addressed.
                                    </p>
                                </div>
                            </div>
                        </center>
                    </body>
                    </html>
                    """;

            default -> "<p>{{message}}</p>";
        };
    }
}