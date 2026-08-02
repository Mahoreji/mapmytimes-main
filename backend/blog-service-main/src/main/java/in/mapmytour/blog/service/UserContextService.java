package in.mapmytour.blog.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service to handle user context from API Gateway headers
 * Gateway adds user information to headers after token validation
 */
@Service
@Slf4j
public class UserContextService {


    @Value("${app.security.gateway-only.enabled:true}")
    private boolean gatewayOnlyEnabled;

    /**
     * Extract user context from request attributes set by JwtAuthenticationFilter
     */
    public UserContext extractUserContext(HttpServletRequest request) {
        if (request == null) {
            log.debug("Request is null, returning null user context");
            return null;
        }

        log.debug("extractUserContext called for request: {}", request.getRequestURI());

        // ALWAYS prefer user info from JWT authentication (set by JwtAuthenticationFilter)
        String jwtUserId = (String) request.getAttribute("jwt.userId");
        String jwtEmail = (String) request.getAttribute("jwt.email");
        String jwtRole = (String) request.getAttribute("jwt.role");
        Boolean jwtIsVerified = (Boolean) request.getAttribute("jwt.isVerified");

        log.debug("JWT attributes - userId: {}, email: {}, role: {}", jwtUserId, jwtEmail, jwtRole);

        if (StringUtils.hasText(jwtUserId) && StringUtils.hasText(jwtEmail)) {
            log.debug("Using validated JWT user context for user: {}", jwtEmail);
            return UserContext.builder()
                    .userId(jwtUserId)
                    .email(jwtEmail)
                    .role(StringUtils.hasText(jwtRole) ? jwtRole : "USER")
                    .authenticated(true)
                    .isVerified(jwtIsVerified != null ? jwtIsVerified : false)
                    .build();
        }

        // NO FALLBACK to API Gateway headers (security requirement: enforce JWT validation)

        // For testing/development: ONLY if gateway-only is disabled, allow test headers
        if (!gatewayOnlyEnabled) {
            String testUserId = request.getHeader("X-Test-User-Id");
            String testEmail = request.getHeader("X-Test-User-Email");
            String testRole = request.getHeader("X-Test-User-Role");
            
            if (StringUtils.hasText(testUserId)) {
                log.debug("Using test user context for development");
                return UserContext.builder()
                        .userId(testUserId)
                        .email(StringUtils.hasText(testEmail) ? testEmail : "test@example.com")
                        .role(StringUtils.hasText(testRole) ? testRole : "USER")
                        .authenticated(true)
                        .build();
            }

            log.debug("No authentication found and gateway-only disabled, using default test user");
            return UserContext.builder()
                    .userId("test-user-123")
                    .email("test@mapmytimes.com")
                    .role("USER")
                    .authenticated(true)
                    .build();
        }

        log.debug("No valid authentication found and gateway-only enabled, returning null user context");
        return null;
    }

    /**
     * Check if the current request is authenticated
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        // Verify via SecurityContext (populated by JwtAuthenticationFilter)
        if (request.getAttribute("jwt.userId") != null) {
            return true;
        }

        // For testing/development: only if gateway-only is disabled
        if (!gatewayOnlyEnabled) {
            String testUserId = request.getHeader("X-Test-User-Id");
            return StringUtils.hasText(testUserId);
        }

        return false;
    }

    /**
     * Get the current user ID from request headers
     */
    public String getCurrentUserId(HttpServletRequest request) {
        log.debug("getCurrentUserId called for request: {}", request.getRequestURI());
        UserContext context = extractUserContext(request);
        log.debug("getCurrentUserId - context: {}, userId: {}", context, context != null ? context.getUserId() : null);
        return context != null ? context.getUserId() : null;
    }

    /**
     * Get the current user email from request headers
     */
    public String getCurrentUserEmail(HttpServletRequest request) {
        UserContext context = extractUserContext(request);
        return context != null ? context.getEmail() : null;
    }

    /**
     * Get the current user role from request headers
     */
    public String getCurrentUserRole(HttpServletRequest request) {
        UserContext context = extractUserContext(request);
        return context != null ? context.getRole() : null;
    }

    /**
     * Check if current user has admin role
     */
    public boolean isCurrentUserAdmin(HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        log.debug("Checking admin role for user. Role: {}", role);
        log.debug("JWT attributes - userId: {}, email: {}, role: {}", 
                request.getAttribute("jwt.userId"), 
                request.getAttribute("jwt.email"), 
                request.getAttribute("jwt.role"));
        return "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
    }

    /**
     * Check if current user has specific role
     */
    public boolean hasRole(HttpServletRequest request, String requiredRole) {
        String userRole = getCurrentUserRole(request);
        return requiredRole.equals(userRole);
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
        private boolean isVerified;
    }
}
