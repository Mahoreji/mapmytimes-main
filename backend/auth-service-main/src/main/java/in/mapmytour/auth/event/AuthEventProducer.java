package in.mapmytour.auth.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Auth Event Producer
 * Publishes authentication and authorization events to Kafka for async processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducer {

    private static final String AUTH_EVENTS_TOPIC = "auth-events";
    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Use a separate executor for Kafka publishes to ensure they don't block the main thread
    private static final Executor kafkaExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kafka-publisher-auth");
        t.setDaemon(true);
        return t;
    });

    /**
     * Publish user registered event
     */
    public void publishUserRegistered(String userId, String email, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_REGISTERED");
        event.put("userId", userId);
        event.put("email", email);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(AUTH_EVENTS_TOPIC, userId, event);
        
        // Also publish notification event
        publishNotificationEvent("USER_REGISTERED", userId, email, 
                "Welcome! Your account has been created successfully.", correlationId);
    }

    /**
     * Publish user login event
     */
    public void publishUserLogin(String userId, String email, String ipAddress, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_LOGIN");
        event.put("userId", userId);
        event.put("email", email);
        event.put("ipAddress", ipAddress);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(AUTH_EVENTS_TOPIC, userId, event);
    }

    /**
     * Publish user logout event
     */
    public void publishUserLogout(String userId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_LOGOUT");
        event.put("userId", userId);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(AUTH_EVENTS_TOPIC, userId, event);
    }

    /**
     * Publish password reset requested event
     */
    public void publishPasswordResetRequested(String userId, String email, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PASSWORD_RESET_REQUESTED");
        event.put("userId", userId);
        event.put("email", email);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(AUTH_EVENTS_TOPIC, userId, event);
        
        // Also publish notification event
        publishNotificationEvent("PASSWORD_RESET_REQUESTED", userId, email,
                "Password reset link has been sent to your email.", correlationId);
    }

    /**
     * Publish password changed event
     */
    public void publishPasswordChanged(String userId, String email, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PASSWORD_CHANGED");
        event.put("userId", userId);
        event.put("email", email);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(AUTH_EVENTS_TOPIC, userId, event);
        
        // Also publish notification event
        publishNotificationEvent("PASSWORD_CHANGED", userId, email,
                "Your password has been changed successfully.", correlationId);
    }

    /**
     * Publish email verified event
     */
    public void publishEmailVerified(String userId, String email, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "EMAIL_VERIFIED");
        event.put("userId", userId);
        event.put("email", email);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(AUTH_EVENTS_TOPIC, userId, event);
        
        // Also publish notification event
        publishNotificationEvent("EMAIL_VERIFIED", userId, email,
                "Your email has been verified successfully.", correlationId);
    }

    private void publishNotificationEvent(String notificationType, String userId, String email, 
                                         String message, String correlationId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", notificationType);
        notification.put("userId", userId);
        notification.put("email", email);
        notification.put("message", message);
        notification.put("correlationId", correlationId);
        notification.put("timestamp", System.currentTimeMillis());
        
        publishEvent(NOTIFICATION_EVENTS_TOPIC, userId, notification);
    }

    private void publishEvent(String topic, String key, Object event) {
        // OPTIMIZATION: Publish asynchronously in a separate thread to ensure it never blocks
        // This is fire-and-forget analytics - failures should not affect auth performance
        kafkaExecutor.execute(() -> {
            try {
                log.debug("Publishing event to topic={}, key={}, eventType={}",
                        topic, key, ((Map<String, Object>) event).get("eventType"));

                CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, event);

                // Set a short timeout (2 seconds) to prevent indefinite blocking
                future.orTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                    .whenComplete((result, ex) -> {
                        if (ex == null && result != null) {
                            log.debug("Event published successfully: topic={}, partition={}, offset={}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else if (ex != null) {
                            // WARN so DNS / connectivity errors surface in production logs
                            // without blocking the auth request (fire-and-forget pattern)
                            String cause = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                            log.warn("[Kafka] Non-blocking publish failure — topic={}, key={}, error={}"
                                    + " | Check KAFKA_BOOTSTRAP_SERVERS and ensure auth-service is on"
                                    + " the same Docker network as the kafka container.",
                                    topic, key, cause);
                        } else {
                            log.warn("[Kafka] Non-blocking publish timeout (2 s) — topic={}, key={}", topic, key);
                        }
                    });
            } catch (Exception e) {
                // WARN so connectivity problems are visible — auth is NOT affected
                log.warn("[Kafka] Non-blocking publish error — topic={}, key={}, error={}"
                        + " | Verify KAFKA_BOOTSTRAP_SERVERS resolves correctly.",
                        topic, key, e.getMessage());
            }
        });
        // Return immediately - the publish happens in background thread
    }
}
