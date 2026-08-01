package in.mapmytour.auth.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

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
     * Extract user context.
     * Priority:
     * 1) API Gateway headers (X-Authenticated=true, X-User-Id, X-User-Email,
     * X-User-Role)
     * 2) Attributes / SecurityContext set by JwtAuthenticationFilter for direct JWT
     * calls
     */
    public UserContext extractUserContext(HttpServletRequest request) {
        if (request == null) {
            log.debug("Request is null, returning null user context");
            return null;
        }

        // 1) Try API Gateway headers first
        String authenticatedHeader = request.getHeader(AUTHENTICATED_HEADER);
        if ("true".equals(authenticatedHeader)) {
            String userId = request.getHeader(USER_ID_HEADER);
            String email = request.getHeader(USER_EMAIL_HEADER);
            String role = request.getHeader(USER_ROLE_HEADER);

            if (StringUtils.hasText(userId) && StringUtils.hasText(email)) {
                UserContext context = UserContext.builder()
                        .userId(userId)
                        .email(email)
                        .role(role)
                        .authenticated(true)
                        .build();

                log.debug("Extracted user context from gateway headers: {}", context);
                return context;
            } else {
                log.warn("Missing required user context headers despite X-Authenticated=true");
            }
        }

        // 2) Fallback: build context from request attributes / SecurityContext (direct
        // JWT auth)
        String attrUserId = (String) request.getAttribute(USER_ID_HEADER);
        String attrEmail = (String) request.getAttribute(USER_EMAIL_HEADER);
        String attrRole = (String) request.getAttribute(USER_ROLE_HEADER);

        if (!StringUtils.hasText(attrEmail) || !StringUtils.hasText(attrRole)) {
            // If attributes are not set, try SecurityContext — but skip anonymous sessions
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)
                    && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {

                if (!StringUtils.hasText(attrEmail)) {
                    attrEmail = String.valueOf(authentication.getPrincipal());
                }

                if (!StringUtils.hasText(attrRole) && authentication.getAuthorities() != null
                        && !authentication.getAuthorities().isEmpty()) {
                    boolean isSuperAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().endsWith("SUPER_ADMIN"));
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().endsWith("ADMIN"));

                    if (isSuperAdmin) {
                        attrRole = "SUPER_ADMIN";
                    } else if (isAdmin) {
                        attrRole = "ADMIN";
                    } else {
                        attrRole = authentication.getAuthorities().iterator().next().getAuthority()
                                .replaceFirst("^ROLE_", "");
                    }
                }
            }
        }

        if (!StringUtils.hasText(attrEmail)) {
            log.debug("No gateway headers or SecurityContext authentication found");
            return null;
        }

        UserContext context = UserContext.builder()
                .userId(attrUserId)
                .email(attrEmail)
                .role(attrRole)
                .authenticated(true)
                .build();

        log.debug("Extracted user context from SecurityContext/attributes: {}", context);
        return context;
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
    public String getCurrentUserId(HttpServletRequest request) {
        UserContext context = extractUserContext(request);
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
    }
}