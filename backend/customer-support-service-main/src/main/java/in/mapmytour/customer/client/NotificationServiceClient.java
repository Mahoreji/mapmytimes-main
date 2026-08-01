package in.mapmytour.customer.client;

import in.mapmytour.customer.dto.SendNotificationRequest;
import in.mapmytour.customer.dto.SendNotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign Client for Notification Service Integration
 * Handles email, SMS, and push notifications
 * Notification service API: POST /api/v1/notification/send
 */
@FeignClient(
    name = "notification-service",
    url = "${notification.service.url:http://notification-service:9090}"
)
public interface NotificationServiceClient {

    /**
     * Send notification (email, SMS, or push)
     * @param source Request source
     * @param timestamp Gateway timestamp
     * @param signature HMAC signature
     * @param request Notification request with type, recipient, subject, body
     * @return Response indicating success/failure
     */
    @PostMapping("/api/v1/notification/send")
    SendNotificationResponse sendNotification(
        @RequestHeader("X-Request-Source") String source,
        @RequestHeader("X-Gateway-Timestamp") String timestamp,
        @RequestHeader("X-Gateway-Signature") String signature,
        @RequestBody SendNotificationRequest request
    );
}

