package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/api/v1/notification", "/api/v1/notifications"})
@Slf4j
public class NotificationController {

    private static final Map<String, Object> EMPTY_UNREAD = Map.of("unread", 0);
    private static final List<Object> EMPTY_LIST = List.of();
    private static final Map<String, Object> EMPTY_STATS;
    static {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("total", 0);
        s.put("unread", 0);
        s.put("read", 0);
        s.put("lastNotificationAt", null);
        EMPTY_STATS = Collections.unmodifiableMap(s);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<APIResponse<Map<String, Object>>> unreadCount() {
        return ResponseEntity.ok(APIResponse.<Map<String, Object>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Unread count retrieved")
                .data(EMPTY_UNREAD)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<Map<String, Object>>> listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("content", EMPTY_LIST);
        pageData.put("totalElements", 0);
        pageData.put("totalPages", 0);
        pageData.put("number", page);
        pageData.put("size", size);
        return ResponseEntity.ok(APIResponse.<Map<String, Object>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Notifications retrieved")
                .data(pageData)
                .build());
    }

    @PostMapping("/contact-form")
    public ResponseEntity<APIResponse<Map<String, Object>>> submitContactForm(
            @RequestBody Map<String, Object> body) {
        String email = body == null ? null : Objects.toString(body.get("email"), null);
        String subject = body == null ? null : Objects.toString(body.get("subject"), null);
        log.info("Contact form received [email={}, subject={}]; delegating to customer-support pipeline.",
                email == null ? "N/A" : email,
                subject == null ? "N/A" : subject);
        Map<String, Object> ack = Map.of(
                "ticketId", "MMT-" + Long.toHexString(System.currentTimeMillis()),
                "receivedAt", new Date().toString(),
                "message", "Thank you — the MapMyTimes newsroom has received your message."
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(APIResponse.<Map<String, Object>>builder()
                        .success(true)
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .message("Contact form submitted successfully")
                        .data(ack)
                        .build());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<APIResponse<Object>> markRead(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.<Object>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Notification marked as read: " + id)
                .data(null)
                .build());
    }

    @PatchMapping("/read-all")
    public ResponseEntity<APIResponse<Object>> markAllRead() {
        return ResponseEntity.ok(APIResponse.<Object>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("All notifications marked as read")
                .data(null)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Object>> deleteNotification(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.<Object>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Notification deleted: " + id)
                .data(null)
                .build());
    }

    @DeleteMapping
    public ResponseEntity<APIResponse<Object>> deleteAll() {
        return ResponseEntity.ok(APIResponse.<Object>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("All notifications deleted")
                .data(null)
                .build());
    }

    @PostMapping({"/send", "/send/instant"})
    public ResponseEntity<APIResponse<Map<String, Object>>> sendNotification(
            @RequestBody Map<String, Object> body) {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("id", UUID.randomUUID().toString());
        stub.put("status", "queued");
        stub.put("deliveredAt", null);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(APIResponse.<Map<String, Object>>builder()
                        .success(true)
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .message("Notification queued")
                        .data(stub)
                        .build());
    }

    @GetMapping("/stats")
    public ResponseEntity<APIResponse<Map<String, Object>>> stats() {
        return ResponseEntity.ok(APIResponse.<Map<String, Object>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Notification stats retrieved")
                .data(EMPTY_STATS)
                .build());
    }
}
