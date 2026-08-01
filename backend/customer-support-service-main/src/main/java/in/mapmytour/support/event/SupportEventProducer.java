package in.mapmytour.support.event;

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
 * Support Event Producer
 * Publishes customer support ticket events to Kafka for async processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupportEventProducer {

    private static final String SUPPORT_EVENTS_TOPIC = "support-events";
    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Use a separate executor for Kafka publishes to ensure they don't block the main thread
    private static final Executor kafkaExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kafka-publisher-support");
        t.setDaemon(true);
        return t;
    });

    /**
     * Publish ticket created event
     */
    public void publishTicketCreated(String ticketId, String userId, String subject, 
                                    String priority, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TICKET_CREATED");
        event.put("ticketId", ticketId);
        event.put("userId", userId);
        event.put("subject", subject);
        event.put("priority", priority);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(SUPPORT_EVENTS_TOPIC, ticketId, event);
        
        // Also publish notification event
        publishNotificationEvent("TICKET_CREATED", userId, 
                String.format("Support ticket #%s has been created.", ticketId), correlationId);
    }

    /**
     * Publish ticket updated event
     */
    public void publishTicketUpdated(String ticketId, String userId, String status, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TICKET_UPDATED");
        event.put("ticketId", ticketId);
        event.put("userId", userId);
        event.put("status", status);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(SUPPORT_EVENTS_TOPIC, ticketId, event);
    }

    /**
     * Publish ticket resolved event
     */
    public void publishTicketResolved(String ticketId, String userId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TICKET_RESOLVED");
        event.put("ticketId", ticketId);
        event.put("userId", userId);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(SUPPORT_EVENTS_TOPIC, ticketId, event);
        
        // Also publish notification event
        publishNotificationEvent("TICKET_RESOLVED", userId,
                String.format("Support ticket #%s has been resolved.", ticketId), correlationId);
    }

    /**
     * Publish ticket escalated event
     */
    public void publishTicketEscalated(String ticketId, String userId, String escalationReason, 
                                      String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TICKET_ESCALATED");
        event.put("ticketId", ticketId);
        event.put("userId", userId);
        event.put("escalationReason", escalationReason);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(SUPPORT_EVENTS_TOPIC, ticketId, event);
    }

    private void publishNotificationEvent(String notificationType, String userId, 
                                         String message, String correlationId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", notificationType);
        notification.put("userId", userId);
        notification.put("message", message);
        notification.put("correlationId", correlationId);
        notification.put("timestamp", System.currentTimeMillis());
        
        publishEvent(NOTIFICATION_EVENTS_TOPIC, userId, notification);
    }

    private void publishEvent(String topic, String key, Object event) {
        // OPTIMIZATION: Publish asynchronously in a separate thread to ensure it never blocks
        // This is fire-and-forget analytics - failures should not affect support performance
        kafkaExecutor.execute(() -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) event;
                log.debug("Publishing event to topic={}, key={}, eventType={}", 
                        topic, key, eventMap.get("eventType"));
                
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
                        } else {
                            // Log at debug level - Kafka failures shouldn't block support
                            if (ex != null && ex.getMessage() != null && 
                                (ex.getMessage().contains("timeout") || ex.getMessage().contains("Connection"))) {
                                log.debug("Kafka not available or timeout (non-blocking): topic={}, key={}", topic, key);
                            } else {
                                log.debug("Failed to publish event (non-blocking): topic={}, key={}, error={}", 
                                        topic, key, ex != null ? ex.getMessage() : "timeout");
                            }
                        }
                    });
            } catch (Exception e) {
                // Log at debug level - analytics shouldn't block support
                log.debug("Error publishing event (non-blocking): topic={}, key={}, error={}", 
                        topic, key, e.getMessage());
            }
        });
        // Return immediately - the publish happens in background thread
    }
}
