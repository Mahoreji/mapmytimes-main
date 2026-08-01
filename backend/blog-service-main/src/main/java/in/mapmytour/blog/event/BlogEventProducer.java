package in.mapmytour.blog.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Blog Event Producer
 * Publishes blog post events to Kafka for async processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogEventProducer {

    private static final String BLOG_EVENTS_TOPIC = "blog-events";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish blog post created event (non-blocking)
     */
    @Async("mediaUploadExecutor")
    public CompletableFuture<Void> publishBlogPostCreated(String postId, String title, String authorId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "BLOG_POST_CREATED");
        event.put("postId", postId);
        event.put("title", title);
        event.put("authorId", authorId);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(BLOG_EVENTS_TOPIC, postId, event);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Publish blog post updated event (non-blocking)
     */
    @Async("mediaUploadExecutor")
    public CompletableFuture<Void> publishBlogPostUpdated(String postId, String title, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "BLOG_POST_UPDATED");
        event.put("postId", postId);
        event.put("title", title);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(BLOG_EVENTS_TOPIC, postId, event);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Publish blog post published event (non-blocking)
     */
    @Async("mediaUploadExecutor")
    public CompletableFuture<Void> publishBlogPostPublished(String postId, String title, String authorId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "BLOG_POST_PUBLISHED");
        event.put("postId", postId);
        event.put("title", title);
        event.put("authorId", authorId);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(BLOG_EVENTS_TOPIC, postId, event);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Publish blog post deleted event (non-blocking)
     */
    @Async("mediaUploadExecutor")
    public CompletableFuture<Void> publishBlogPostDeleted(String postId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "BLOG_POST_DELETED");
        event.put("postId", postId);
        event.put("correlationId", correlationId);
        event.put("timestamp", System.currentTimeMillis());
        
        publishEvent(BLOG_EVENTS_TOPIC, postId, event);
        return CompletableFuture.completedFuture(null);
    }

    private void publishEvent(String topic, String key, Object event) {
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
                    } else {
                        // Log at debug level - Kafka failures shouldn't block blog operations
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
            // Log at debug level - analytics shouldn't block blog operations
            log.debug("Error publishing event (non-blocking): topic={}, key={}, error={}", 
                    topic, key, e.getMessage());
        }
    }
}
