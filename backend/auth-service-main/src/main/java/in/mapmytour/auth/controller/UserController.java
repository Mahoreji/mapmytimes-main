package in.mapmytour.auth.controller;

import in.mapmytour.auth.dto.APIResponse;
import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.auth.UserStatsResponse;
import in.mapmytour.auth.dto.user.*;
import in.mapmytour.auth.dto.user.AdminActivityItemResponse;
import in.mapmytour.auth.exception.ProfileNotPublicException;
import in.mapmytour.auth.service.UserService;
import in.mapmytour.auth.service.UserContextService;
import in.mapmytour.auth.utils.APIResponseUtil;
import in.mapmytour.auth.utils.FileUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserContextService userContextService;
    private final SimpMessagingTemplate messagingTemplate;
    private final in.mapmytour.auth.helper.RealtimeNotificationHelper realtimeNotificationHelper;

    // ================ USER MANAGEMENT ================
    /**
     * Get User by userId
     * If authenticated, will show connection count if viewing own profile or if
     * profile is public
     */
    @GetMapping("/{userId}")
    public ResponseEntity<APIResponse<UserProfileResponse>> getUserById(
            @PathVariable String userId,
            HttpServletRequest request) {
        try {
            // Try to get current user for context (optional - for connection count
            // visibility)
            String currentUserEmail = userContextService.getCurrentUserEmail(request);
            UserProfileResponse response = userService.getUserById(userId, currentUserEmail);
            return APIResponseUtil.success(response, "User retrieved successfully");
        } catch (Exception e) {
            log.error("Get user failed", e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }
    // ================ PROFILE MANAGEMENT ================

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<APIResponse<UserProfileResponse>> getCurrentUser(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            UserProfileResponse response = userService.getCurrentUser(userEmail);
            return APIResponseUtil.success(response, "Profile retrieved successfully");
        } catch (Exception e) {
            log.error("Get profile failed", e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<APIResponse<UserProfileResponse>> getPublicProfile(@PathVariable String userId) {
        try {
            UserProfileResponse response = userService.getPublicProfile(userId);
            return APIResponseUtil.success(response, "Public profile retrieved successfully");
        } catch (ProfileNotPublicException e) {
            log.warn("Profile access restricted: {} for user {}", e.getMessage(), userId);
            return APIResponseUtil.forbidden(e.getMessage());
        } catch (Exception e) {
            log.error("Get public profile failed for user {}", userId, e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get public connections of a specific user
     */
    @GetMapping("/profile/{userId}/connections")
    public ResponseEntity<APIResponse<List<UserConnectionResponse>>> getPublicConnections(@PathVariable String userId) {
        try {
            List<UserConnectionResponse> responses = userService.getPublicConnections(userId);
            return APIResponseUtil.success(responses, "Public connections retrieved successfully");
        } catch (ProfileNotPublicException e) {
            log.warn("Profile connections access restricted: {} for user {}", e.getMessage(), userId);
            return APIResponseUtil.forbidden(e.getMessage());
        } catch (Exception e) {
            log.error("Get public connections failed for user {}", userId, e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get mutual connections between current user and a specific user
     */
    @GetMapping("/profile/{userId}/mutual")
    public ResponseEntity<APIResponse<List<UserConnectionResponse>>> getMutualConnections(
            @PathVariable String userId,
            HttpServletRequest request) {
        String currentUserEmail = userContextService.getCurrentUserEmail(request);
        if (currentUserEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<UserConnectionResponse> response = userService.getMutualConnections(currentUserEmail, userId);
            return APIResponseUtil.success(response, "Mutual connections retrieved");
        } catch (Exception e) {
            log.error("Get mutual connections failed", e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get summary profile of any user (basic info only)
     */
    @GetMapping("/summary/{userId}")
    public ResponseEntity<APIResponse<UserSummaryResponse>> getUserSummary(@PathVariable String userId) {
        try {
            UserSummaryResponse response = userService.getUserSummary(userId);
            return APIResponseUtil.success(response, "User summary retrieved successfully");
        } catch (Exception e) {
            log.error("Get user summary failed", e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<APIResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            UserProfileResponse response = userService.updateProfile(request, userEmail);
            return APIResponseUtil.success(response, "Profile updated successfully");
        } catch (Exception e) {
            log.error("Profile update failed", e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update profile visibility
     */
    @PatchMapping("/profile/visibility")
    public ResponseEntity<APIResponse<MessageResponse>> updateProfileVisibility(
            @RequestParam boolean isVisible,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.updateProfileVisibility(userEmail, isVisible);
            return APIResponseUtil.success(response, "Profile visibility updated");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ AVATAR MANAGEMENT ================

    /**
     * Upload user avatar
     */
    @PostMapping("/avatar")
    public ResponseEntity<APIResponse<AvatarUploadResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            // Validate file
            String validationError = FileUtil.getValidationErrorMessage(file);
            if (validationError != null) {
                return APIResponseUtil.badRequest(validationError);
            }

            if (!FileUtil.isValidImageFile(file)) {
                return APIResponseUtil.badRequest("Invalid image file. Only JPEG, PNG, GIF, WebP images are allowed.");
            }

            AvatarUploadResponse response = userService.uploadAvatar(file, userEmail);
            return APIResponseUtil.success(response, "Avatar uploaded successfully");
        } catch (Exception e) {
            log.error("Avatar upload failed", e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete user avatar
     */
    @DeleteMapping("/avatar")
    public ResponseEntity<APIResponse<MessageResponse>> deleteAvatar(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.deleteAvatar(userEmail);
            return APIResponseUtil.success(response, "Avatar deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ COVER IMAGE MANAGEMENT ================

    /**
     * Upload user cover image
     */
    @PostMapping("/cover-image")
    public ResponseEntity<APIResponse<AvatarUploadResponse>> uploadCoverImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            // Validate file
            String validationError = FileUtil.getValidationErrorMessage(file);
            if (validationError != null) {
                return APIResponseUtil.badRequest(validationError);
            }

            if (!FileUtil.isValidImageFile(file)) {
                return APIResponseUtil.badRequest("Invalid image file. Only JPEG, PNG, GIF, WebP images are allowed.");
            }

            AvatarUploadResponse response = userService.uploadCoverImage(file, userEmail);
            return APIResponseUtil.success(response, "Cover image uploaded successfully");
        } catch (Exception e) {
            log.error("Cover image upload failed", e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete user cover image
     */
    @DeleteMapping("/cover-image")
    public ResponseEntity<APIResponse<MessageResponse>> deleteCoverImage(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.deleteCoverImage(userEmail);
            return APIResponseUtil.success(response, "Cover image deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Generate avatar URL with initials
     */
    @GetMapping("/avatar/generate")
    public ResponseEntity<APIResponse<String>> generateAvatarUrl(
            @RequestParam String initials,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            String avatarUrl = userService.generateAvatarUrl(userEmail, initials);
            return APIResponseUtil.success(avatarUrl, "Avatar URL generated");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ ADDRESS MANAGEMENT ================

    /**
     * Update user address
     */
    @PatchMapping("/address")
    public ResponseEntity<APIResponse<UserProfileResponse>> updateAddress(
            @Valid @RequestBody AddressRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            UserProfileResponse response = userService.updateAddress(request, userEmail);
            return APIResponseUtil.success(response, "Address updated successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete user address
     */
    @DeleteMapping("/address")
    public ResponseEntity<APIResponse<MessageResponse>> deleteAddress(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.deleteAddress(userEmail);
            return APIResponseUtil.success(response, "Address deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get saved addresses
     */
    @GetMapping("/addresses")
    public ResponseEntity<APIResponse<List<AddressResponse>>> getSavedAddresses(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<AddressResponse> response = userService.getSavedAddresses(userEmail);
            return APIResponseUtil.success(response, "Saved addresses retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Save new address
     */
    @PostMapping("/addresses")
    public ResponseEntity<APIResponse<MessageResponse>> saveAddress(
            @Valid @RequestBody AddressRequest request,
            @RequestParam String label,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.saveAddress(request, userEmail, label);
            return APIResponseUtil.success(response, "Address saved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ PREFERENCES MANAGEMENT ================

    /**
     * Update user preferences
     */
    @PatchMapping("/preferences")
    public ResponseEntity<APIResponse<UserProfileResponse>> updatePreferences(
            @Valid @RequestBody UserPreferencesRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            UserProfileResponse response = userService.updatePreferences(request, userEmail);
            return APIResponseUtil.success(response, "Preferences updated successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get user preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<APIResponse<UserPreferencesResponse>> getPreferences(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            UserPreferencesResponse response = userService.getPreferences(userEmail);
            return APIResponseUtil.success(response, "Preferences retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Reset preferences to default
     */
    @PostMapping("/preferences/reset")
    public ResponseEntity<APIResponse<MessageResponse>> resetPreferencesToDefault(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.resetPreferencesToDefault(userEmail);
            return APIResponseUtil.success(response, "Preferences reset to default");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ NOTIFICATION SETTINGS ================

    /**
     * Get notification settings
     */
    @GetMapping("/notifications/settings")
    public ResponseEntity<APIResponse<NotificationSettingsResponse>> getNotificationSettings(
            HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            NotificationSettingsResponse response = userService.getNotificationSettings(userEmail);
            return APIResponseUtil.success(response, "Notification settings retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update notification settings
     */
    @PutMapping("/notifications/settings")
    public ResponseEntity<APIResponse<MessageResponse>> updateNotificationSettings(
            @Valid @RequestBody NotificationSettingsRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.updateNotificationSettings(request, userEmail);
            return APIResponseUtil.success(response, "Notification settings updated");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Subscribe to notification type
     */
    @PostMapping("/notifications/subscribe")
    public ResponseEntity<APIResponse<MessageResponse>> subscribeToNotification(
            @RequestParam String notificationType,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.subscribeToNotification(userEmail, notificationType);
            return APIResponseUtil.success(response, "Subscribed to notification");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Unsubscribe from notification type
     */
    @PostMapping("/notifications/unsubscribe")
    public ResponseEntity<APIResponse<MessageResponse>> unsubscribeFromNotification(
            @RequestParam String notificationType,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.unsubscribeFromNotification(userEmail, notificationType);
            return APIResponseUtil.success(response, "Unsubscribed from notification");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ PRIVACY SETTINGS ================

    /**
     * Get privacy settings
     */
    @GetMapping("/privacy/settings")
    public ResponseEntity<APIResponse<PrivacySettingsResponse>> getPrivacySettings(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            PrivacySettingsResponse response = userService.getPrivacySettings(userEmail);
            return APIResponseUtil.success(response, "Privacy settings retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update privacy settings
     */
    @PutMapping("/privacy/settings")
    public ResponseEntity<APIResponse<MessageResponse>> updatePrivacySettings(
            @Valid @RequestBody PrivacySettingsRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.updatePrivacySettings(request, userEmail);
            return APIResponseUtil.success(response, "Privacy settings updated");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Block user
     */
    @PostMapping("/privacy/block")
    public ResponseEntity<APIResponse<MessageResponse>> blockUser(
            @RequestParam String targetUserId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.blockUser(userEmail, targetUserId);
            return APIResponseUtil.success(response, "User blocked successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Unblock user
     */
    @PostMapping("/privacy/unblock")
    public ResponseEntity<APIResponse<MessageResponse>> unblockUser(
            @RequestParam String targetUserId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.unblockUser(userEmail, targetUserId);
            return APIResponseUtil.success(response, "User unblocked successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get blocked users
     */
    @GetMapping("/privacy/blocked")
    public ResponseEntity<APIResponse<List<BlockedUserResponse>>> getBlockedUsers(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<BlockedUserResponse> response = userService.getBlockedUsers(userEmail);
            return APIResponseUtil.success(response, "Blocked users retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ DOCUMENT MANAGEMENT ================

    /**
     * Upload document
     */
    @PostMapping("/documents")
    public ResponseEntity<APIResponse<DocumentUploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            if (!FileUtil.isValidDocumentFile(file)) {
                return APIResponseUtil.badRequest("Invalid document file. Only PDF, DOC, DOCX files are allowed.");
            }

            DocumentUploadResponse response = userService.uploadDocument(file, documentType, userEmail);
            return APIResponseUtil.success(response, "Document uploaded successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get user documents
     */
    @GetMapping("/documents")
    public ResponseEntity<APIResponse<List<DocumentResponse>>> getDocuments(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<DocumentResponse> response = userService.getDocuments(userEmail);
            return APIResponseUtil.success(response, "Documents retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get specific document
     */
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<APIResponse<DocumentResponse>> getDocument(
            @PathVariable String documentId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            DocumentResponse response = userService.getDocument(userEmail, documentId);
            return APIResponseUtil.success(response, "Document retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete document
     */
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<APIResponse<MessageResponse>> deleteDocument(
            @PathVariable String documentId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.deleteDocument(userEmail, documentId);
            return APIResponseUtil.success(response, "Document deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ ACTIVITY & STATISTICS ================

    /**
     * Get user activity
     */
    @GetMapping("/activity")
    public ResponseEntity<APIResponse<UserActivityResponse>> getUserActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            UserActivityResponse response = userService.getUserActivity(userEmail, pageable);
            return APIResponseUtil.success(response, "User activity retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Track user activity (package views, clicks, etc.)
     */
    @PostMapping("/activity/track")
    public ResponseEntity<APIResponse<MessageResponse>> trackActivity(
            @Valid @RequestBody TrackActivityRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            MessageResponse response = userService.trackActivity(request, userEmail, ipAddress, userAgent);
            return APIResponseUtil.success(response, "Activity tracked successfully");
        } catch (Exception e) {
            log.error("Failed to track activity: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get user statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<APIResponse<UserStatsResponse>> getUserStatistics(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            UserStatsResponse response = userService.getUserStatistics(userEmail);
            return APIResponseUtil.success(response, "User statistics retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get all user activities (Admin only)
     * Can optionally filter by user email
     */
    @GetMapping("/admin/activities")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<Page<AdminActivityItemResponse>>> getAllUserActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest request) {

        if (!userContextService.isCurrentUserAdmin(request)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<AdminActivityItemResponse> response = userService.getAllUserActivities(pageable, userEmail);
            return APIResponseUtil.success(response, "All user activities retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to retrieve all user activities: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get login history
     */
    @GetMapping("/login-history")
    public ResponseEntity<APIResponse<LoginHistoryResponse>> getLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("loginTime").descending());
            LoginHistoryResponse response = userService.getLoginHistory(userEmail, pageable);
            return APIResponseUtil.success(response, "Login history retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ SOCIAL FEATURES ================

    /**
     * Get user connections
     */
    @GetMapping("/connections")
    public ResponseEntity<APIResponse<List<UserConnectionResponse>>> getConnections(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        log.debug("MAIN USER_EMAIL: {}", userEmail);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<UserConnectionResponse> response = userService.getConnections(userEmail);
            return APIResponseUtil.success(response, "Connections retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get incoming (received) connection requests for the current user
     */
    @GetMapping("/connections/requests/incoming")
    public ResponseEntity<APIResponse<List<NotificationItemResponse>>> getIncomingConnectionRequests(
            HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<NotificationItemResponse> response = userService.getIncomingConnectionRequests(userEmail);
            return APIResponseUtil.success(response, "Incoming connection requests and notifications retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get outgoing (sent) connection requests for the current user
     */
    @GetMapping("/connections/requests/outgoing")
    public ResponseEntity<APIResponse<List<ConnectionRequestResponse>>> getOutgoingConnectionRequests(
            HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<ConnectionRequestResponse> response = userService.getOutgoingConnectionRequests(userEmail);
            return APIResponseUtil.success(response, "Outgoing connection requests retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Send connection request
     */
    @PostMapping("/connections/request")
    public ResponseEntity<APIResponse<MessageResponse>> sendConnectionRequest(
            @RequestParam String targetUserId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.sendConnectionRequest(userEmail, targetUserId);
            return APIResponseUtil.success(response, "Connection request sent");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Accept connection request
     */
    @PostMapping("/connections/accept")
    public ResponseEntity<APIResponse<MessageResponse>> acceptConnectionRequest(
            @RequestParam String requestId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.acceptConnectionRequest(userEmail, requestId);
            return APIResponseUtil.success(response, "Connection request accepted");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Reject connection request
     */
    @PostMapping("/connections/reject")
    public ResponseEntity<APIResponse<MessageResponse>> rejectConnectionRequest(
            @RequestParam String requestId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.rejectConnectionRequest(userEmail, requestId);
            return APIResponseUtil.success(response, "Connection request rejected");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Withdraw connection request (cancel a request you sent)
     */
    @RequestMapping(value = "/connections/withdraw", method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE})
    public ResponseEntity<APIResponse<MessageResponse>> withdrawConnectionRequest(
            @RequestParam String requestId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.withdrawConnectionRequest(userEmail, requestId);
            return APIResponseUtil.success(response, "Connection request withdrawn");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Remove connection
     */
    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<APIResponse<MessageResponse>> removeConnection(
            @PathVariable String connectionId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.removeConnection(userEmail, connectionId);
            return APIResponseUtil.success(response, "Connection removed");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ SEARCH & DISCOVERY ================

    /**
     * Search users
     */
    @GetMapping("/search")
    public ResponseEntity<APIResponse<Page<UserProfileResponse>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        String currentUserId = userContextService.getCurrentUserId(request);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserProfileResponse> response = userService.searchUsers(query, pageable, currentUserId);
            return APIResponseUtil.success(response, "User search completed");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get suggested users
     */
    @GetMapping("/suggestions")
    public ResponseEntity<APIResponse<List<UserSuggestionResponse>>> getSuggestedUsers(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<UserSuggestionResponse> response = userService.getSuggestedUsers(userEmail);
            return APIResponseUtil.success(response, "User suggestions retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get nearby users (by destination from travel plans)
     */
    @GetMapping("/nearby")
    public ResponseEntity<APIResponse<List<UserProfileResponse>>> getNearbyUsers(
            @RequestParam(defaultValue = "10.0") double radius,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<UserProfileResponse> response = userService.getNearbyUsers(userEmail, radius);
            return APIResponseUtil.success(response, "Nearby users retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get nearby travelers going to same destination (with optional location
     * filter)
     */
    @GetMapping("/travelers/nearby")
    public ResponseEntity<APIResponse<List<UserProfileResponse>>> getNearbyTravelers(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String filterByLocation, // CITY, STATE, COUNTRY
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            // If destination not specified, use user's travel plans
            if (destination == null || destination.isEmpty()) {
                // Fall back to the original nearby users endpoint
                List<UserProfileResponse> response = userService.getNearbyUsers(userEmail, 10.0);
                return APIResponseUtil.success(response, "Nearby travelers retrieved");
            }

            List<UserProfileResponse> response = userService.getNearbyTravelers(
                    userEmail, destination, filterByLocation);
            return APIResponseUtil.success(response, "Nearby travelers retrieved for destination: " + destination);
        } catch (Exception e) {
            log.error("Failed to get nearby travelers: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ TRAVEL PLANS ================

    /**
     * Create travel plan
     */
    @PostMapping("/travel-plans")
    public ResponseEntity<APIResponse<TravelPlanResponse>> createTravelPlan(
            @Valid @RequestBody TravelPlanRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelPlanResponse response = userService.createTravelPlan(request, userEmail);
            return APIResponseUtil.success(response, "Travel plan created successfully");
        } catch (Exception e) {
            log.error("Failed to create travel plan: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get user's travel plans
     */
    @GetMapping("/travel-plans")
    public ResponseEntity<APIResponse<List<TravelPlanResponse>>> getTravelPlans(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<TravelPlanResponse> response = userService.getTravelPlans(userEmail);
            return APIResponseUtil.success(response, "Travel plans retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update travel plan
     */
    @PutMapping("/travel-plans/{planId}")
    public ResponseEntity<APIResponse<TravelPlanResponse>> updateTravelPlan(
            @PathVariable String planId,
            @Valid @RequestBody TravelPlanRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelPlanResponse response = userService.updateTravelPlan(planId, request, userEmail);
            return APIResponseUtil.success(response, "Travel plan updated successfully");
        } catch (Exception e) {
            log.error("Failed to update travel plan: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete travel plan
     */
    @DeleteMapping("/travel-plans/{planId}")
    public ResponseEntity<APIResponse<MessageResponse>> deleteTravelPlan(
            @PathVariable String planId,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.deleteTravelPlan(planId, userEmail);
            return APIResponseUtil.success(response, "Travel plan deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Find travelers by destination (public endpoint)
     */
    @GetMapping("/travel-plans/destination/{destination}")
    public ResponseEntity<APIResponse<List<TravelPlanResponse>>> findTravelersByDestination(
            @PathVariable String destination) {

        try {
            List<TravelPlanResponse> response = userService.findTravelersByDestination(destination);
            return APIResponseUtil.success(response, "Travelers found for destination: " + destination);
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ TRAVEL GROUPS ================

    /**
     * Create travel group (JSON body)
     */
    @PostMapping(value = "/travel-groups", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse<TravelGroupResponse>> createTravelGroup(
            @Valid @RequestBody TravelGroupRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelGroupResponse response = userService.createTravelGroup(request, userEmail);
            return APIResponseUtil.success(response, "Travel group created successfully");
        } catch (Exception e) {
            log.error("Failed to create travel group: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Create travel group with image upload (multipart/form-data).
     * Fields of TravelGroupRequest are sent as form fields and the image as "file".
     */
    @PostMapping(value = "/travel-groups", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<TravelGroupResponse>> createTravelGroupWithImage(
            @ModelAttribute TravelGroupRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelGroupResponse response = userService.createTravelGroupWithImage(request, file, userEmail);
            return APIResponseUtil.success(response, "Travel group created successfully");
        } catch (Exception e) {
            log.error("Failed to create travel group with image: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get travel group by ID
     */
    @GetMapping("/travel-groups/{groupId}")
    public ResponseEntity<APIResponse<TravelGroupResponse>> getTravelGroup(
            @PathVariable String groupId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelGroupResponse response = userService.getTravelGroup(groupId, userEmail);
            return APIResponseUtil.success(response, "Travel group retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get my travel groups
     */
    @GetMapping("/travel-groups")
    public ResponseEntity<APIResponse<List<TravelGroupResponse>>> getMyTravelGroups(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<TravelGroupResponse> response = userService.getMyTravelGroups(userEmail);
            return APIResponseUtil.success(response, "Travel groups retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update travel group
     */
    @PutMapping("/travel-groups/{groupId}")
    public ResponseEntity<APIResponse<TravelGroupResponse>> updateTravelGroup(
            @PathVariable String groupId,
            @Valid @RequestBody TravelGroupRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelGroupResponse response = userService.updateTravelGroup(groupId, request, userEmail);
            return APIResponseUtil.success(response, "Travel group updated successfully");
        } catch (Exception e) {
            log.error("Failed to update travel group: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete travel group
     */
    @DeleteMapping("/travel-groups/{groupId}")
    public ResponseEntity<APIResponse<MessageResponse>> deleteTravelGroup(
            @PathVariable String groupId,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.deleteTravelGroup(groupId, userEmail);
            return APIResponseUtil.success(response, "Travel group deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Join travel group by invite code
     */
    @PostMapping("/travel-groups/join")
    public ResponseEntity<APIResponse<MessageResponse>> joinTravelGroup(
            @RequestParam String inviteCode,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.joinTravelGroup(inviteCode, userEmail);
            return APIResponseUtil.success(response, "Successfully joined travel group");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Leave travel group
     */
    @PostMapping("/travel-groups/{groupId}/leave")
    public ResponseEntity<APIResponse<MessageResponse>> leaveTravelGroup(
            @PathVariable String groupId,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.leaveTravelGroup(groupId, userEmail);
            return APIResponseUtil.success(response, "Successfully left travel group");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Invite user to travel group
     */
    @PostMapping("/travel-groups/{groupId}/invite")
    public ResponseEntity<APIResponse<MessageResponse>> inviteToTravelGroup(
            @PathVariable String groupId,
            @RequestParam String inviteeEmail,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.inviteToTravelGroup(groupId, userEmail, inviteeEmail);
            return APIResponseUtil.success(response, "Invitation sent successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Discover travel groups by destination
     */
    @GetMapping("/travel-groups/discover")
    public ResponseEntity<APIResponse<List<TravelGroupResponse>>> discoverTravelGroups(
            @RequestParam String destination,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<TravelGroupResponse> response = userService.discoverTravelGroups(destination, userEmail);
            return APIResponseUtil.success(response, "Travel groups discovered successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ TRAVEL ITINERARY ================

    /**
     * Create itinerary item
     */
    @PostMapping("/itinerary")
    public ResponseEntity<APIResponse<TravelItineraryResponse>> createItineraryItem(
            @Valid @RequestBody TravelItineraryRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelItineraryResponse response = userService.createItineraryItem(request, userEmail);
            return APIResponseUtil.success(response, "Itinerary item created successfully");
        } catch (Exception e) {
            log.error("Failed to create itinerary item: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get itinerary (with optional date range)
     */
    @GetMapping("/itinerary")
    public ResponseEntity<APIResponse<List<TravelItineraryResponse>>> getItinerary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
            List<TravelItineraryResponse> response = userService.getItinerary(userEmail, start, end);
            return APIResponseUtil.success(response, "Itinerary retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get itinerary by specific date
     */
    @GetMapping("/itinerary/date")
    public ResponseEntity<APIResponse<List<TravelItineraryResponse>>> getItineraryByDate(
            @RequestParam String date,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            LocalDate itineraryDate = LocalDate.parse(date);
            List<TravelItineraryResponse> response = userService.getItineraryByDate(userEmail, itineraryDate);
            return APIResponseUtil.success(response, "Itinerary retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update itinerary item
     */
    @PutMapping("/itinerary/{itemId}")
    public ResponseEntity<APIResponse<TravelItineraryResponse>> updateItineraryItem(
            @PathVariable String itemId,
            @Valid @RequestBody TravelItineraryRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelItineraryResponse response = userService.updateItineraryItem(itemId, request, userEmail);
            return APIResponseUtil.success(response, "Itinerary item updated successfully");
        } catch (Exception e) {
            log.error("Failed to update itinerary item: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete itinerary item
     */
    @DeleteMapping("/itinerary/{itemId}")
    public ResponseEntity<APIResponse<MessageResponse>> deleteItineraryItem(
            @PathVariable String itemId,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.deleteItineraryItem(itemId, userEmail);
            return APIResponseUtil.success(response, "Itinerary item deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get group itinerary
     */
    @GetMapping("/travel-groups/{groupId}/itinerary")
    public ResponseEntity<APIResponse<List<TravelItineraryResponse>>> getGroupItinerary(
            @PathVariable String groupId,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<TravelItineraryResponse> response = userService.getGroupItinerary(groupId, userEmail);
            return APIResponseUtil.success(response, "Group itinerary retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ TRIP MATCHING ================

    /**
     * Find travel matches for a destination
     */
    @GetMapping("/travel-matches")
    public ResponseEntity<APIResponse<List<TravelMatchResponse>>> findTravelMatches(
            @RequestParam String destination,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<TravelMatchResponse> response = userService.findTravelMatches(userEmail, destination);
            return APIResponseUtil.success(response, "Travel matches found successfully");
        } catch (Exception e) {
            log.error("Failed to find travel matches: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get personalized travel matches
     */
    @GetMapping("/travel-matches/personalized")
    public ResponseEntity<APIResponse<List<TravelMatchResponse>>> getPersonalizedMatches(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<TravelMatchResponse> response = userService.getPersonalizedMatches(userEmail);
            return APIResponseUtil.success(response, "Personalized matches retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to get personalized matches: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ VERIFICATION ================

    /**
     * Submit verification request
     */
    @PostMapping("/verification/submit")
    public ResponseEntity<APIResponse<MessageResponse>> submitVerificationRequest(
            @Valid @RequestBody VerificationRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.submitVerificationRequest(request, userEmail);
            return APIResponseUtil.success(response, "Verification request submitted");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get verification status
     */
    @GetMapping("/verification/status")
    public ResponseEntity<APIResponse<VerificationStatusResponse>> getVerificationStatus(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            VerificationStatusResponse response = userService.getVerificationStatus(userEmail);
            return APIResponseUtil.success(response, "Verification status retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Upload verification document
     */
    @PostMapping("/verification/document")
    public ResponseEntity<APIResponse<MessageResponse>> uploadVerificationDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.uploadVerificationDocument(file, documentType, userEmail);
            return APIResponseUtil.success(response, "Verification document uploaded");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ ACCOUNT MANAGEMENT ================

    /**
     * Delete user account
     */
    @DeleteMapping("/account")
    public ResponseEntity<APIResponse<MessageResponse>> deleteAccount(
            @RequestParam String password,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.deleteAccount(password, userEmail);
            return APIResponseUtil.success(response, "Account deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Request account verification
     */
    @PostMapping("/account/request-verification")
    public ResponseEntity<APIResponse<MessageResponse>> requestAccountVerification(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.requestAccountVerification(userEmail);
            return APIResponseUtil.success(response, "Account verification requested");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Export account data
     */
    @GetMapping("/account/export")
    public ResponseEntity<APIResponse<AccountDataResponse>> exportAccountData(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            AccountDataResponse response = userService.exportAccountData(userEmail);
            return APIResponseUtil.success(response, "Account data exported");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Import account data
     */
    @PostMapping("/account/import")
    public ResponseEntity<APIResponse<MessageResponse>> importAccountData(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.importAccountData(file, userEmail);
            return APIResponseUtil.success(response, "Account data imported");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ INTERESTS MANAGEMENT ================

    /**
     * Get user interests
     */
    @GetMapping("/interests")
    public ResponseEntity<APIResponse<List<InterestResponse>>> getInterests(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<InterestResponse> response = userService.getInterests(userEmail);
            return APIResponseUtil.success(response, "Interests retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update user interests
     */
    @PutMapping("/interests")
    public ResponseEntity<APIResponse<MessageResponse>> updateInterests(
            @RequestBody List<String> interests,
            HttpServletRequest request) {

        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.updateInterests(interests, userEmail);
            return APIResponseUtil.success(response, "Interests updated");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get available interests
     */
    @GetMapping("/interests/available")
    public ResponseEntity<APIResponse<List<InterestResponse>>> getAvailableInterests() {
        try {
            List<InterestResponse> response = userService.getAvailableInterests();
            return APIResponseUtil.success(response, "Available interests retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ FEEDBACK ================

    /**
     * Submit feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<APIResponse<MessageResponse>> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = userService.submitFeedback(request, userEmail);
            return APIResponseUtil.success(response, "Feedback submitted");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get user feedback
     */
    @GetMapping("/feedback")
    public ResponseEntity<APIResponse<List<FeedbackResponse>>> getUserFeedback(HttpServletRequest request) {
        String userEmail = userContextService.getCurrentUserEmail(request);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<FeedbackResponse> response = userService.getUserFeedback(userEmail);
            return APIResponseUtil.success(response, "User feedback retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ ADMIN ENDPOINTS ================

    /**
     * Get all users for admin
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<Page<UserProfileResponse>>> getAllUsersForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @ModelAttribute UserFilterRequest filter,
            HttpServletRequest request) {

        if (!userContextService.isCurrentUserAdmin(request)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<UserProfileResponse> response = userService.getAllUsersForAdmin(pageable, filter);
            return APIResponseUtil.success(response, "Users retrieved for admin");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get user by ID for internal services
     */
    @GetMapping("/internal/users/{userId}")
    public ResponseEntity<APIResponse<UserProfileResponse>> getUserByIdForInternal(
            @PathVariable String userId,
            HttpServletRequest request) {
        return getUserByIdForAdmin(userId, request);
    }

    /**
     * Get user by ID for admin or internal services
     */
    @GetMapping("/admin/users/{userId}")
    public ResponseEntity<APIResponse<UserProfileResponse>> getUserByIdForAdmin(
            @PathVariable String userId,
            HttpServletRequest request) {

        log.info("Fetching user details for ID: {} (Admin/Internal request)", userId);

        // Security Check: Allow internal service calls or Admin users
        String requestSource = request.getHeader("X-Request-Source");
        boolean isInternalCall = "internal-service".equalsIgnoreCase(requestSource);
        boolean isAdmin = userContextService.isCurrentUserAdmin(request);

        if (!isInternalCall && !isAdmin) {
            log.warn("Blocked unauthenticated/non-admin attempt to fetch user details for: {}", userId);
            return APIResponseUtil.forbidden("Admin or internal service access required");
        }

        try {
            UserProfileResponse response = userService.getUserById(userId);
            return APIResponseUtil.success(response, "User retrieved successfully");
        } catch (IllegalArgumentException e) {
            return APIResponseUtil.notFound(e.getMessage());
        } catch (Exception e) {
            log.error("Error retrieving user details for admin: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest("Error retrieving user details");
        }
    }

    /**
     * Admin update user profile
     */
    @PutMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> adminUpdateUserProfile(
            @PathVariable String userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            MessageResponse response = userService.adminUpdateUserProfile(userId, request);
            return APIResponseUtil.success(response, "User profile updated by admin");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ ADMIN VERIFICATION MANAGEMENT ================

    /**
     * Get all verification requests (Admin only)
     */
    @GetMapping("/admin/verification/requests")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<Page<VerificationRequestResponse>>> getAllVerificationRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        if (!userContextService.isCurrentUserAdmin(request)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<VerificationRequestResponse> response = userService.getAllVerificationRequests(pageable, status);
            return APIResponseUtil.success(response, "Verification requests retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get verification request by ID (Admin only)
     */
    @GetMapping("/admin/verification/requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<VerificationRequestResponse>> getVerificationRequestById(
            @PathVariable String requestId,
            HttpServletRequest request) {

        if (!userContextService.isCurrentUserAdmin(request)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            VerificationRequestResponse response = userService.getVerificationRequestById(requestId);
            return APIResponseUtil.success(response, "Verification request retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Approve verification request (Admin only)
     */
    @PostMapping("/admin/verification/requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> approveVerificationRequest(
            @PathVariable String requestId,
            @RequestBody(required = false) AdminVerificationActionRequest actionRequest,
            HttpServletRequest httpRequest) {

        String adminEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (adminEmail == null || !userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            String adminNotes = actionRequest != null ? actionRequest.getAdminNotes() : null;
            MessageResponse response = userService.approveVerificationRequest(requestId, adminEmail, adminNotes);
            return APIResponseUtil.success(response, "Verification request approved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Reject verification request (Admin only)
     */
    @PostMapping("/admin/verification/requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> rejectVerificationRequest(
            @PathVariable String requestId,
            @RequestBody(required = false) AdminVerificationActionRequest actionRequest,
            HttpServletRequest httpRequest) {

        String adminEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (adminEmail == null || !userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            String adminNotes = actionRequest != null ? actionRequest.getAdminNotes() : null;
            MessageResponse response = userService.rejectVerificationRequest(requestId, adminEmail, adminNotes);
            return APIResponseUtil.success(response, "Verification request rejected");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ UTILITY METHODS ================


    // ================ NOTIFICATION MANAGEMENT ================

    /**
     * Internal endpoint for other services (like blog-service) to push social notifications
     */
    @PostMapping("/internal/notifications")
    public ResponseEntity<APIResponse<MessageResponse>> notifySocialAction(
            @RequestBody InternalNotificationRequest internalRequest) {
        
        // In a real microservice world, we'd verify this comes from an internal source
        // For now, we'll just process it
        try {
            userService.createSocialNotification(
                internalRequest.getRecipientUserId(), 
                internalRequest.getSenderUserId(), 
                internalRequest.getType(), 
                internalRequest.getMessage(), 
                internalRequest.getPostId(),
                internalRequest.getBookingId(),
                internalRequest.getPaymentId(),
                internalRequest.getActionUrl());
            return APIResponseUtil.success(null, "Notification processed");
        } catch (Exception e) {
            log.error("Failed to process social notification", e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<APIResponse<MessageResponse>> healthCheck() {
        try {
            MessageResponse response = userService.healthCheck();
            return APIResponseUtil.success(response, "User service is healthy");
        } catch (Exception e) {
            return APIResponseUtil.internalServerError("Health check failed");
        }
    }

    // =============== GROUP MESSAGING ================

    /**
     * Send a message to a travel group
     */
    @PostMapping("/travel-groups/{groupId}/messages")
    public ResponseEntity<APIResponse<GroupMessageResponse>> sendGroupMessage(
            @PathVariable String groupId,
            @Valid @RequestBody GroupMessageRequest request,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            GroupMessageResponse response = userService.sendGroupMessage(groupId, request, userEmail);

            // Broadcast to all WebSocket subscribers of this group (real-time)
            try {
                String topic = "/topic/group/" + groupId;
                log.debug("Broadcasting group message to topic: {}", topic);
                messagingTemplate.convertAndSend(topic, response);
                log.debug("Successfully broadcasted group message to: {}", topic);
            } catch (Exception e) {
                log.error("Failed to broadcast group message over WebSocket: {}", e.getMessage(), e);
            }

            return APIResponseUtil.success(response, "Message sent successfully");
        } catch (Exception e) {
            log.error("Failed to send group message: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get messages from a travel group
     */
    @GetMapping("/travel-groups/{groupId}/messages")
    public ResponseEntity<APIResponse<Page<GroupMessageResponse>>> getGroupMessages(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<GroupMessageResponse> response = userService.getGroupMessages(groupId, userEmail, pageable);
            return APIResponseUtil.success(response, "Messages retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to get group messages: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // =============== DIRECT MESSAGING ================

    /**
     * Send a direct message to another user
     */
    @PostMapping("/messages/direct")
    public ResponseEntity<APIResponse<DirectMessageResponse>> sendDirectMessage(
            @Valid @RequestBody DirectMessageRequest request,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            DirectMessageResponse response = userService.sendDirectMessage(request, userEmail);

            // Push to recipient and sender WebSocket queues in real-time
            try {
                String recipientEmail = request.getRecipientEmail().trim();
                String senderEmail = userEmail.trim();

                log.debug("Broadcasting direct message - Recipient: {}, Sender: {}", recipientEmail, senderEmail);

                // Get sender user for notification
                var senderProfile = userService.getCurrentUser(userEmail);

                // Prepare message data for unified notification
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("messageId", response.getId());
                messageData.put("message", response.getMessage());
                messageData.put("messageType", response.getMessageType());
                messageData.put("senderId", response.getSenderId());
                messageData.put("recipientId", response.getRecipientId());
                messageData.put("timestamp", response.getCreatedAt());

                // Create sender user entity for notification
                in.mapmytour.auth.entity.User senderUser = in.mapmytour.auth.entity.User.builder()
                        .id(senderProfile.getId())
                        .email(senderProfile.getEmail())
                        .firstName(senderProfile.getFirstName())
                        .lastName(senderProfile.getLastName())
                        .avatarUrl(senderProfile.getAvatarUrl())
                        .build();

                // Send unified notification to recipient
                realtimeNotificationHelper.sendMessageNotification(
                        recipientEmail,
                        senderUser,
                        response.getId(),
                        messageData);

                // Send message payload to recipient (for real-time conversation)
                messagingTemplate.convertAndSendToUser(
                        recipientEmail,
                        "/queue/messages",
                        response);
                log.debug("Sent direct message to recipient queue: /user/{}/queue/messages", recipientEmail);

                // Send message payload to sender (echo for real-time conversation)
                messagingTemplate.convertAndSendToUser(
                        senderEmail,
                        "/queue/messages",
                        response);
                log.debug("Sent direct message to sender queue: /user/{}/queue/messages", senderEmail);
            } catch (Exception e) {
                log.error("Failed to broadcast direct message over WebSocket: {}", e.getMessage(), e);
            }

            return APIResponseUtil.success(response, "Message sent successfully");
        } catch (Exception e) {
            log.error("Failed to send direct message: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get conversation with another user
     */
    @GetMapping("/messages/conversation/{recipientEmail}")
    public ResponseEntity<APIResponse<Page<DirectMessageResponse>>> getConversation(
            @PathVariable String recipientEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DirectMessageResponse> response = userService.getConversation(recipientEmail, userEmail, pageable);
            return APIResponseUtil.success(response, "Conversation retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to get conversation: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get list of conversation partners
     */
    @GetMapping("/messages/partners")
    public ResponseEntity<APIResponse<List<UserProfileResponse>>> getConversationPartners(
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            List<UserProfileResponse> response = userService.getConversationPartners(userEmail);
            return APIResponseUtil.success(response, "Conversation partners retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to get conversation partners: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Mark a message as read
     */
    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<APIResponse<MessageResponse>> markMessageAsRead(
            @PathVariable String messageId,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            MessageResponse response = userService.markMessageAsRead(messageId, userEmail);
            return APIResponseUtil.success(response, "Message marked as read");
        } catch (Exception e) {
            log.error("Failed to mark message as read: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get unread message count
     */
    @GetMapping("/messages/unread/count")
    public ResponseEntity<APIResponse<Long>> getUnreadMessageCount(HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            long count = userService.getUnreadMessageCount(userEmail);
            return APIResponseUtil.success(count, "Unread message count retrieved");
        } catch (Exception e) {
            log.error("Failed to get unread message count: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // =============== EXPENSE MANAGEMENT ================

    /**
     * Create a group expense
     */
    @PostMapping("/travel-groups/{groupId}/expenses")
    public ResponseEntity<APIResponse<GroupExpenseResponse>> createGroupExpense(
            @PathVariable String groupId,
            @RequestBody GroupExpenseRequest request,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            GroupExpenseResponse response = userService.createGroupExpense(groupId, request, userEmail);
            return APIResponseUtil.success(response, "Expense created successfully");
        } catch (Exception e) {
            log.error("Failed to create group expense: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Upload or update travel group image
     */
    @PostMapping("/travel-groups/{groupId}/image")
    public ResponseEntity<APIResponse<TravelGroupResponse>> uploadTravelGroupImage(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            TravelGroupResponse response = userService.uploadGroupImage(groupId, file, userEmail);
            return APIResponseUtil.success(response, "Travel group image uploaded successfully");
        } catch (Exception e) {
            log.error("Failed to upload travel group image: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get all expenses for a travel group
     */
    @GetMapping("/travel-groups/{groupId}/expenses")
    public ResponseEntity<APIResponse<List<GroupExpenseResponse>>> getGroupExpenses(
            @PathVariable String groupId,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            List<GroupExpenseResponse> response = userService.getGroupExpenses(groupId, userEmail);
            return APIResponseUtil.success(response, "Expenses retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to get group expenses: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get expense details
     */
    @GetMapping("/expenses/{expenseId}")
    public ResponseEntity<APIResponse<GroupExpenseResponse>> getExpenseDetails(
            @PathVariable String expenseId,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            GroupExpenseResponse response = userService.getExpenseDetails(expenseId, userEmail);
            return APIResponseUtil.success(response, "Expense details retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to get expense details: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update an expense
     */
    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<APIResponse<MessageResponse>> updateExpense(
            @PathVariable String expenseId,
            @RequestBody GroupExpenseRequest request,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            MessageResponse response = userService.updateExpense(expenseId, request, userEmail);
            return APIResponseUtil.success(response, "Expense updated successfully");
        } catch (Exception e) {
            log.error("Failed to update expense: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete an expense
     */
    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<APIResponse<MessageResponse>> deleteExpense(
            @PathVariable String expenseId,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            MessageResponse response = userService.deleteExpense(expenseId, userEmail);
            return APIResponseUtil.success(response, "Expense deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete expense: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Record a payment for an expense
     */
    @PostMapping("/expenses/{expenseId}/payments")
    public ResponseEntity<APIResponse<MessageResponse>> recordPayment(
            @PathVariable String expenseId,
            @RequestParam String participantId,
            @RequestParam BigDecimal amount,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            MessageResponse response = userService.recordPayment(expenseId, participantId, amount, userEmail);
            return APIResponseUtil.success(response, "Payment recorded successfully");
        } catch (Exception e) {
            log.error("Failed to record payment: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get expense settlement summary for a group
     */
    @GetMapping("/travel-groups/{groupId}/expenses/settlement")
    public ResponseEntity<APIResponse<ExpenseSettlementResponse>> getExpenseSettlement(
            @PathVariable String groupId,
            HttpServletRequest httpRequest) {
        String userEmail = userContextService.getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            ExpenseSettlementResponse response = userService.getExpenseSettlement(groupId, userEmail);
            return APIResponseUtil.success(response, "Expense settlement retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to get expense settlement: {}", e.getMessage(), e);
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }
}