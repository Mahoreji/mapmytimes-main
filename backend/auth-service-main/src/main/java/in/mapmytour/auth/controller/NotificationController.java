package in.mapmytour.auth.controller;

import in.mapmytour.auth.dto.APIResponse;
import in.mapmytour.auth.dto.user.NotificationResponse;
import in.mapmytour.auth.service.NotificationService;
import in.mapmytour.auth.service.UserContextService;
import in.mapmytour.auth.utils.APIResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final UserContextService userContextService;

    /**
     * Get notification history for the current user
     */
    @GetMapping
    public ResponseEntity<APIResponse<List<NotificationResponse>>> getNotificationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<NotificationResponse> history = notificationService.getNotificationHistory(userEmail, page, size);
            return APIResponseUtil.success(history, "Notification history retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to get notification history for user {}", userEmail, e);
            return APIResponseUtil.badRequest("Failed to retrieve notifications: " + e.getMessage());
        }
    }

    /**
     * Mark a specific notification as read
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<APIResponse<Boolean>> markAsRead(
            @PathVariable String notificationId,
            HttpServletRequest request) {
        
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        boolean success = notificationService.markNotificationAsRead(userEmail, notificationId);
        if (success) {
            return APIResponseUtil.success(true, "Notification marked as read");
        } else {
            return APIResponseUtil.badRequest("Notification not found or access denied");
        }
    }

    /**
     * Mark all notifications as read for current user
     */
    @PatchMapping("/read-all")
    public ResponseEntity<APIResponse<Boolean>> markAllAsRead(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        boolean success = notificationService.markAllNotificationsAsRead(userEmail);
        return APIResponseUtil.success(success, "All notifications marked as read");
    }
}
