package in.mapmytour.customer.service;

import in.mapmytour.customer.exception.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service to handle user context from API Gateway headers
 * Gateway adds user information to headers after token validation
 */
@Service
@Slf4j
public class UserContextService {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String AUTHENTICATED_HEADER = "X-Authenticated";

    /**
     * Get current request from thread local
     */
    public HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * Extract user context from request headers set by API Gateway
     */
    public UserContext extractUserContext(HttpServletRequest request) {
        if (request == null) {
            log.debug("Request is null, returning null user context");
            return null;
        }

        String authenticated = request.getHeader(AUTHENTICATED_HEADER);
        if (!"true".equals(authenticated)) {
            log.debug("Request is not authenticated by gateway");
            return null;
        }

        String userId = request.getHeader(USER_ID_HEADER);
        String email = request.getHeader(USER_EMAIL_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        if (!StringUtils.hasText(userId) || !StringUtils.hasText(email)) {
            log.warn("Missing required user context headers");
            return null;
        }

        UserContext context = UserContext.builder()
                .userId(userId)
                .email(email)
                .role(role)
                .authenticated(true)
                .build();

        log.debug("Extracted user context: {}", context);
        return context;
    }

    /**
     * Get current user context from current request
     */
    public UserContext getCurrentUserContext() {
        return extractUserContext(getCurrentRequest());
    }

    /**
     * Check if the current request is authenticated by gateway
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return "true".equals(request.getHeader(AUTHENTICATED_HEADER));
    }

    /**
     * Get the current user ID from request headers
     */
    public String getCurrentUserId() {
        UserContext context = getCurrentUserContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * Get the current user email from request headers
     */
    public String getCurrentUserEmail() {
        UserContext context = getCurrentUserContext();
        return context != null ? context.getEmail() : null;
    }

    /**
     * Get the current user role from request headers
     */
    public String getCurrentUserRole() {
        UserContext context = getCurrentUserContext();
        return context != null ? context.getRole() : null;
    }

    /**
     * Check if current user has admin role
     */
    public boolean isCurrentUserAdmin() {
        String role = getCurrentUserRole();
        return "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
    }

    /**
     * Check if current user has specific role
     */
    public boolean hasRole(String requiredRole) {
        String userRole = getCurrentUserRole();
        return requiredRole.equals(userRole);
    }

    /**
     * Check if current user is the owner of the resource
     */
    public boolean isOwner(String resourceOwnerId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(resourceOwnerId);
    }

    /**
     * Validate if current user can access resource
     */
    public void validateAccess(String resourceOwnerId) {
        if (!isCurrentUserAdmin() && !isOwner(resourceOwnerId)) {
            throw new AccessDeniedException(
                    "Access denied: You don't have permission to access this resource");
        }
    }

    /**
     * User context data class
     */
    @Data
    @Builder
    public static class UserContext {
        private String userId;
        private String email;
        private String role;
        private boolean authenticated;
    }
}