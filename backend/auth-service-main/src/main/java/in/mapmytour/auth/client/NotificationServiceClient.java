package in.mapmytour.auth.client;

import in.mapmytour.auth.dto.notification.SendNotificationRequest;
import in.mapmytour.auth.dto.notification.SendNotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "notification-service",
    url = "${app.external.notification-service.url:http://notification-service:9090}",
    path = "/api/v1"
)
public interface NotificationServiceClient {

    @PostMapping("/notification/send/instant")
    SendNotificationResponse sendInstantNotification(
        @RequestBody SendNotificationRequest request,
        @RequestHeader("X-Request-Source") String requestSource,
        @RequestHeader("X-Gateway-Timestamp") String timestamp,
        @RequestHeader("X-Gateway-Signature") String signature
    );
    
    @PostMapping("/notification/send")
    SendNotificationResponse sendNotification(
        @RequestBody SendNotificationRequest request,
        @RequestHeader("X-Request-Source") String requestSource,
        @RequestHeader("X-Gateway-Timestamp") String timestamp,
        @RequestHeader("X-Gateway-Signature") String signature
    );
}
