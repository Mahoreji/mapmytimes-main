package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.client.NotificationServiceClient;
import in.mapmytour.customer.dto.SendNotificationRequest;
import in.mapmytour.customer.dto.SendNotificationResponse;
import in.mapmytour.customer.service.NotificationService;
import in.mapmytour.customer.utils.SignatureUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of NotificationService
 * Integrates with notification-service for sending emails, SMS, and push notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationServiceClient notificationServiceClient;

    @Value("${app.notification.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${app.security.gateway-secret:${GATEWAY_JWT_SECRET:secret}}")
    private String gatewaySecret;

    @Override
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendTicketCreatedNotificationFallback")
    @Retry(name = "notificationService")
    public void sendTicketCreatedNotification(String ticketId, String customerId, String customerEmail, String subject) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled, skipping ticket created notification for: {}", ticketId);
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ticketId", ticketId);
        metadata.put("customerId", customerId);

        SendNotificationRequest request = SendNotificationRequest.builder()
            .type("email")
            .recipient(customerEmail)
            .subject("Support Ticket Created - " + subject)
            .body(String.format(
                "Your support ticket #%s has been created successfully.\n\nSubject: %s\n\nWe will get back to you soon.",
                ticketId, subject
            ))
            .source("customer-support-service")
            .metadata(metadata)
            .build();

        try {
            sendWithSecurity(request);
            log.debug("Ticket created notification sent to: {}", customerEmail);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification via Feign", e);
        }
    }

    /**
     * Fallback method for sendTicketCreatedNotification
     */
    public void sendTicketCreatedNotificationFallback(String ticketId, String customerId, String customerEmail, String subject, Throwable e) {
        log.warn("Using fallback for ticket created notification: {} due to error: {}", ticketId, e.getMessage());
        // Log to database or queue for retry later
    }

    @Override
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendTicketUpdatedNotificationFallback")
    @Retry(name = "notificationService")
    public void sendTicketUpdatedNotification(String ticketId, String customerId, String customerEmail, String updateMessage) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled, skipping ticket updated notification for: {}", ticketId);
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ticketId", ticketId);
        metadata.put("customerId", customerId);

        SendNotificationRequest request = SendNotificationRequest.builder()
            .type("email")
            .recipient(customerEmail)
            .subject("Support Ticket Updated - Ticket #" + ticketId)
            .body(String.format(
                "Your support ticket #%s has been updated.\n\n%s",
                ticketId, updateMessage
            ))
            .source("customer-support-service")
            .metadata(metadata)
            .build();

        try {
            sendWithSecurity(request);
            log.debug("Ticket updated notification sent to: {}", customerEmail);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification via Feign", e);
        }
    }

    /**
     * Fallback method for sendTicketUpdatedNotification
     */
    public void sendTicketUpdatedNotificationFallback(String ticketId, String customerId, String customerEmail, String updateMessage, Throwable e) {
        log.warn("Using fallback for ticket updated notification: {} due to error: {}", ticketId, e.getMessage());
    }

    @Override
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendTicketResolvedNotificationFallback")
    @Retry(name = "notificationService")
    public void sendTicketResolvedNotification(String ticketId, String customerId, String customerEmail, String resolutionMessage) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled, skipping ticket resolved notification for: {}", ticketId);
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ticketId", ticketId);
        metadata.put("customerId", customerId);

        SendNotificationRequest request = SendNotificationRequest.builder()
            .type("email")
            .recipient(customerEmail)
            .subject("Support Ticket Resolved - Ticket #" + ticketId)
            .body(String.format(
                "Your support ticket #%s has been resolved.\n\n%s",
                ticketId, resolutionMessage
            ))
            .source("customer-support-service")
            .metadata(metadata)
            .build();

        try {
            sendWithSecurity(request);
            log.debug("Ticket resolved notification sent to: {}", customerEmail);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification via Feign", e);
        }
    }

    /**
     * Fallback method for sendTicketResolvedNotification
     */
    public void sendTicketResolvedNotificationFallback(String ticketId, String customerId, String customerEmail, String resolutionMessage, Throwable e) {
        log.warn("Using fallback for ticket resolved notification: {} due to error: {}", ticketId, e.getMessage());
    }

    @Override
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendEscalationNotificationFallback")
    @Retry(name = "notificationService")
    public void sendEscalationNotification(String ticketId, String agentId, String agentEmail, String escalationReason) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled, skipping escalation notification for: {}", ticketId);
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ticketId", ticketId);
        metadata.put("agentId", agentId);

        SendNotificationRequest request = SendNotificationRequest.builder()
            .type("email")
            .recipient(agentEmail)
            .subject("Ticket Escalated - Ticket #" + ticketId)
            .body(String.format(
                "Ticket #%s has been escalated.\n\nReason: %s\n\nPlease review and take appropriate action.",
                ticketId, escalationReason
            ))
            .source("customer-support-service")
            .metadata(metadata)
            .build();

        try {
            sendWithSecurity(request);
            log.debug("Escalation notification sent to agent: {}", agentEmail);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification via Feign", e);
        }
    }

    /**
     * Fallback method for sendEscalationNotification
     */
    public void sendEscalationNotificationFallback(String ticketId, String agentId, String agentEmail, String escalationReason, Throwable e) {
        log.warn("Using fallback for escalation notification: {} due to error: {}", ticketId, e.getMessage());
    }

    @Override
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendAgentAssignmentNotificationFallback")
    @Retry(name = "notificationService")
    public void sendAgentAssignmentNotification(String ticketId, String agentId, String agentEmail, String ticketSubject) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled, skipping agent assignment notification for: {}", ticketId);
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ticketId", ticketId);
        metadata.put("agentId", agentId);

        SendNotificationRequest request = SendNotificationRequest.builder()
            .type("email")
            .recipient(agentEmail)
            .subject("New Ticket Assigned - Ticket #" + ticketId)
            .body(String.format(
                "A new support ticket has been assigned to you.\n\nTicket ID: %s\nSubject: %s\n\nPlease review and respond.",
                ticketId, ticketSubject
            ))
            .source("customer-support-service")
            .metadata(metadata)
            .build();

        try {
            sendWithSecurity(request);
            log.debug("Agent assignment notification sent to: {}", agentEmail);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification via Feign", e);
        }
    }

    /**
     * Fallback method for sendAgentAssignmentNotification
     */
    public void sendAgentAssignmentNotificationFallback(String ticketId, String agentId, String agentEmail, String ticketSubject, Throwable e) {
        log.warn("Using fallback for agent assignment notification: {} due to error: {}", ticketId, e.getMessage());
    }

    private SendNotificationResponse sendWithSecurity(SendNotificationRequest request) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Request-Source", "internal-service");
        headers.put("X-Gateway-Timestamp", timestamp);
        
        String signature = SignatureUtils.generateSignature(headers, gatewaySecret);
        
        return notificationServiceClient.sendNotification("internal-service", timestamp, signature, request);
    }
}

