package in.mapmytour.customer.client;

import in.mapmytour.customer.dto.SendNotificationRequest;
import in.mapmytour.customer.dto.SendNotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Fallback implementation for NotificationServiceClient
 * Provides default responses when notification service is unavailable
 */
@Component
@Slf4j
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public SendNotificationResponse sendNotification(String source, String timestamp, String signature, SendNotificationRequest request) {
        log.warn("Notification service unavailable, {} not sent to: {}", request.getType(), request.getRecipient());
        return SendNotificationResponse.builder()
            .success(false)
            .message("Notification service unavailable (fallback)")
            .statusCode(503)
            .data(Collections.singletonMap("error", "Service Unavailable"))
            .build();
    }
}

