package in.mapmytour.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.mapmytour.api.dto.APIResponse;
import in.mapmytour.api.utils.IpUtils;
import in.mapmytour.api.utils.GatewayJwtUtil;
import in.mapmytour.api.service.SecurityCacheService;
import in.mapmytour.api.service.SecurityLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private GatewayJwtUtil jwtUtil;

    @Autowired
    private in.mapmytour.api.utils.GatewaySignatureUtil signatureUtil;

    @Autowired
    private SecurityCacheService securityCacheService;

    @Autowired
    private SecurityLoggingService securityLoggingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            // System endpoints
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/favicon.ico",
            "/error",
            "/health",
            "/fallback/**",

            // ================ AUTH SERVICE PUBLIC ENDPOINTS ================
            // Authentication & Registration
            "/api/v1/auth/oauth2/**",
            "/api/v1/auth/register",
            "/api/v1/auth/register/agent",
            "/api/v1/auth/login",
            "/api/v1/auth/send-otp",
            "/api/v1/auth/login-otp",
            "/api/v1/auth/refresh",
            "/api/v1/auth/validate-token",

            // Password Management
            "/api/v1/auth/forgot-password/step1",
            "/api/v1/auth/forgot-password/step2",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/validate-password",

            // Email Verification
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/send-verification-otp",

            // Two Factor Authentication (verification only)
            "/api/v1/auth/2fa/verify",

            // Account Management (public queries)
            "/api/v1/auth/check-email",
            "/api/v1/auth/account-status",
            "/api/v1/auth/reactivate",
            "/api/v1/auth/confirm-deletion",

            // Security & Monitoring (public reporting)
            "/api/v1/auth/security/report",
            "/api/v1/auth/rate-limit",

            // Health check
            "/api/v1/auth/health",

            // WebSocket endpoints (SockJS handshake)
            "/api/v1/auth/ws/**",
            "/api/v1/auth/ws/info",

            // ================ USER SERVICE PUBLIC ENDPOINTS ================
            // Public Profile & Search
            "/api/v1/user/profile/**", // Public user profiles
            "/api/v1/user/search", // Public user search
            "/api/v1/user/interests/available", // Available interests list
            "/api/v1/user/health", // Health check

            // ================ TRAVEL SERVICE PUBLIC ENDPOINTS ================
            // Flight reference data (public access for search/display)
            "/api/v1/travel/flight/airlines",
            "/api/v1/travel/flight/airlines/*",
            "/api/v1/travel/flight/airlines/code/*",
            "/api/v1/travel/flight/airlines/search",
            "/api/v1/travel/flight/airports",
            "/api/v1/travel/flight/airports/*",
            "/api/v1/travel/flight/airports/code/*",
            "/api/v1/travel/flight/airports/search",
            "/api/v1/travel/flight/search",
            "/api/v1/travel/flight/search/stream",
            "/api/v1/travel/flight/calendar-fare",
            "/api/v1/travel/flight/cancellation-charges",
            "/api/v1/travel/flight/fare-quote",
            "/api/v1/travel/flight/fare-rule",
            "/api/v1/travel/flight/hold-booking",
            "/api/v1/travel/flight/ssr",
            "/api/v1/travel/flight/seat-map",
            // Bus reference data (public access for search/display)
            "/api/v1/travel/bus/origin-dest-mappings",
            "/api/v1/travel/bus/origin-dest-mappings/*",
            "/api/v1/travel/bus/origin-dest-mappings/origin-code/*",
            "/api/v1/travel/bus/origin-dest-mappings/search",
            "/api/v1/travel/bus/search",
            "/api/v1/travel/bus/seat-layout",
            "/api/v1/travel/bus/boarding-points",
            // Hotel reference data (public access for search/display)
            "/api/v1/travel/hotel/city-codes",
            "/api/v1/travel/hotel/city-codes/*",
            "/api/v1/travel/hotel/city-codes/city-id/*",
            "/api/v1/travel/hotel/city-codes/country/*",
            "/api/v1/travel/hotel/city-codes/search",
            "/api/v1/travel/hotel/search",
            "/api/v1/travel/hotel/info",
            "/api/v1/travel/hotel/rooms",

            // ================ OTHER SERVICES PUBLIC ENDPOINTS ================
            // Reviews service public endpoints
            "/api/v1/reviews/search",
            "/api/v1/reviews/entity/*/rating",
            "/api/v1/reviews/entity/*/stats",
            "/api/v1/reviews/entity/*/summary",
            "/api/v1/reviews/entity/*/top-rated",
            "/api/v1/reviews/trending",
            "/api/v1/reviews/recent",
            "/api/v1/reviews/entity/*",

            // Blog service public endpoints - specific endpoints (GET requests handled
            // separately)
            "/api/v1/blog/posts/search",
            "/api/v1/blog/posts/slug/**",
            "/api/v1/blog/categories/hierarchy",
            "/api/v1/blog/categories/slug/**",
            "/api/v1/blog/comments/post/*/approved",
            "/api/v1/blog/likes/post/*/count",
            "/api/v1/blog/tags/popular",
            "/api/v1/blog/tags/slug/**",
            "/api/v1/blog/settings/map",

            // Customer support public endpoints
            "/api/v1/customer/knowledge-base/search",
            "/api/v1/customer/knowledge-base/*/public",
            "/api/v1/customer/knowledge-base/categories",
            "/api/v1/customer/quote-requests/public/**",
            "/api/v1/customer/feedback/stats/public",
            "/api/v1/jobs/**",

            // Utils service - all public
            "/api/v1/utils/**",

            // Core service public endpoints (tours, destinations, activities, adventures)
            "/api/v1/tours/**",
            "/api/v1/group-tours/**",
            "/api/v1/destinations/**",
            "/api/v1/activities/**",
            "/api/v1/adventures/**",
            "/api/v1/inclusions/**",
            "/api/v1/exclusions/**",

            // Booking service public endpoints
            "/api/v1/bookings/public/**",
            "/api/v1/bookings/availability/**",
            "/api/v1/bookings/pricing/**",

            // Hotel service public endpoints
            "/api/v1/hotels/**",

            // Notification service public endpoints
            "/api/v1/health",
            "/api/v1/notification/contact-form",

            // ================ AGENT SERVICE PUBLIC ENDPOINTS ================
            // Agent Onboarding (Public) - Registration is now handled by auth service
            "/api/v1/agent/onboarding",
            // Agent Existence Checks (Public)
            "/api/v1/agent/exists/email/**",
            "/api/v1/agent/exists/code/**",
            "/api/v1/agent/exists/gstin/**",
            // Agent Counts (Public - for display purposes)
            "/api/v1/agent/count/active",
            "/api/v1/agent/count/tier/**",

            // ================ PAYMENT SERVICE PUBLIC ENDPOINTS ================
            "/api/v1/payment/health",
            "/api/v1/payment/health/**",
            "/api/v1/payment/docs/**",
            "/api/v1/payment/redoc/**",
            "/api/v1/payment/cashfree/webhook/**",
            "/api/v1/payment/cashfree/success",
            "/api/v1/payment/cashfree/success/**",

            // ================ BOOKING SERVICE SERVICE-TO-SERVICE ENDPOINTS
            // ================
            "/api/v1/bookings/**/update-payment",
            "/api/v1/bookings/**/update-payment/",
            "/api/v1/bookings/**/add-partial-payment",
            "/api/v1/bookings/**/add-partial-payment/");

    // Admin-only endpoints that require ADMIN or SUPER_ADMIN role
    private static final List<String> ADMIN_ONLY_ENDPOINTS = Arrays.asList(
            // Gateway-level security management
            "/api/v1/admin/security/**",

            // Auth service admin endpoints
            "/api/v1/auth/admin/**",
            "/api/v1/super-admin/**",

            // User service admin endpoints
            "/api/v1/user/admin/**",

            // Core service admin endpoints
            "/api/v1/admin/tours/**",
            "/api/v1/admin/destinations/**",
            "/api/v1/admin/activities/**",
            "/api/v1/admin/adventures/**",

            // Hotel service admin endpoints
            "/api/v1/admin/hotels/**",

            // Other services admin endpoints
            "/api/v1/bookings/admin/**",
            "/api/v1/reviews/admin/**",
            "/api/v1/blog/admin/**",
            "/api/v1/customer/admin/**",
            "/api/v1/payments/admin/**",
            "/api/v1/travel/admin/**",
            "/api/v1/admin/jobs/**",
            "/api/v1/admin/applications/**",

            // Travel service admin endpoints (pricing, financial, dashboard)
            "/api/v1/admin/pricing/**",
            "/api/v1/admin/financial/**",
            "/api/v1/admin/dashboard/**",

            // Travel service balance endpoints (admin only)
            "/api/v1/travel/flight/balance",
            "/api/v1/travel/flight/balance-log",
            "/api/v1/travel/bus/balance",
            "/api/v1/travel/bus/balance-log",
            "/api/v1/travel/hotel/balance",
            "/api/v1/travel/hotel/balance-log",

            // Agent service admin endpoints
            "/api/v1/agent/admin/**");

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            log.debug("Processing {} request for path: {}", method, path);

            // Check if path is public
            boolean isPublic = isPublicEndpoint(path, config.getExcludeUrls()) || isBlogGetRequest(path, method);

            // Handle OPTIONS requests for CORS
            if ("OPTIONS".equals(method)) {
                log.debug("OPTIONS request detected for path: {}, allowing without authentication", path);
                return chain.filter(exchange);
            }

            // Extract JWT token from Authorization header
            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String token = jwtUtil.extractTokenFromHeader(authorizationHeader);

            if (token == null && !isPublic) {
                log.warn("No JWT token found in {} request to private endpoint {}", method, path);
                return handleUnauthorized(exchange, "Authentication token is required for this endpoint",
                        "TOKEN_MISSING");
            }

            try {
                GatewayJwtUtil.UserContext userContext = null;

                if (token != null) {
                    // Extract all claims
                    io.jsonwebtoken.Claims claims = jwtUtil.getAllClaimsFromToken(token);

                    // Validate token
                    if (!jwtUtil.isValidForApiAccess(claims)) {
                        log.warn("Invalid or expired JWT token for {} request to path: {}", method, path);
                        String clientIp = IpUtils.resolveClientIp(request);

                        if (!Boolean.TRUE.equals(exchange.getAttribute(AdminIpWhitelistFilter.WHITELIST_ATTRIBUTE))) {
                            securityCacheService.incrementThreatScore(clientIp, 5).subscribe();
                            securityLoggingService.logEvent(clientIp, null, path, method, "AUTH_FAILURE", "DENIED", 5,
                                    "Invalid or expired JWT token");
                        }

                        return handleUnauthorized(exchange, "Invalid or expired authentication token", "TOKEN_INVALID");
                    }

                    // Extract user context
                    userContext = jwtUtil.extractUserContext(claims);

                    if (userContext == null && !isPublic) {
                        log.warn("Could not extract user context from token for request to private path: {}", path);
                        return handleUnauthorized(exchange, "Invalid authentication token format", "TOKEN_MALFORMED");
                    }
                }

                // Authorization checks for non-public endpoints or if user context is available
                if (userContext != null) {
                    // Check if user has required permissions for admin endpoints
                    if (isAdminOnlyEndpoint(path) && !isAuthorizedForAdminPath(path, method, userContext)) {
                        log.warn("User {} does not have admin permissions for {} request to path: {}",
                                userContext.getEmail(), method, path);
                        return handleForbidden(exchange, "Administrator privileges required for this operation",
                                "INSUFFICIENT_ADMIN_PERMISSIONS");
                    }

                    // Check other specific permissions
                    if (!hasRequiredPermissions(path, method, userContext)) {
                        log.warn("User {} does not have required permissions for {} request to path: {}",
                                userContext.getEmail(), method, path);
                        return handleForbidden(exchange, "Insufficient permissions for this operation",
                                "INSUFFICIENT_PERMISSIONS");
                    }
                }

                // Add headers for downstream services
                String timestamp = String.valueOf(System.currentTimeMillis());
                java.util.Map<String, String> signedHeaders = new java.util.HashMap<>();
                signedHeaders.put("X-Request-Source", "api-gateway");
                signedHeaders.put("X-Gateway-Timestamp", timestamp);

                ServerHttpRequest.Builder requestBuilder = request.mutate()
                        .header("X-Request-Source", "api-gateway")
                        .header("X-Gateway-Timestamp", timestamp);

                if (userContext != null) {
                    signedHeaders.put("X-User-Id", userContext.getUserId());
                    signedHeaders.put("X-User-Email", userContext.getEmail());
                    signedHeaders.put("X-User-Role", userContext.getRole());
                    signedHeaders.put("X-Authenticated", "true");

                    requestBuilder.header("X-User-Id", userContext.getUserId())
                            .header("X-User-Email", userContext.getEmail())
                            .header("X-User-Role", userContext.getRole())
                            .header("X-Authenticated", "true")
                            .header("Authorization", authorizationHeader);
                    
                    if (userContext.getName() != null) {
                        requestBuilder.header("X-User-Name", userContext.getName());
                    }
                }

                String signature = signatureUtil.generateSignature(signedHeaders);
                requestBuilder.header("X-Gateway-Signature", signature);

                log.debug("Forwarding {} request to {}: public={}, authenticated={}",
                        method, path, isPublic, userContext != null);

                return chain.filter(exchange.mutate().request(requestBuilder.build()).build());

            } catch (Exception e) {
                if (!isPublic) {
                    log.error("Error during token validation for private path {}: {}", path, e.getMessage());
                    String clientIp = IpUtils.resolveClientIp(request);
                    securityCacheService.incrementThreatScore(clientIp, 10).subscribe();
                    return handleUnauthorized(exchange, "Authentication failed due to server error", "AUTH_ERROR");
                }

                // If public, continue with basic headers only
                log.debug("Error during token validation for public path {}, continuing unauthenticated: {}", path,
                        e.getMessage());
                String timestamp = String.valueOf(System.currentTimeMillis());
                java.util.Map<String, String> signedHeaders = new java.util.HashMap<>();
                signedHeaders.put("X-Request-Source", "api-gateway");
                signedHeaders.put("X-Gateway-Timestamp", timestamp);
                String signature = signatureUtil.generateSignature(signedHeaders);

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-Request-Source", "api-gateway")
                        .header("X-Gateway-Timestamp", timestamp)
                        .header("X-Gateway-Signature", signature)
                        .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }
        };
    }


    private boolean isPublicEndpoint(String path, List<String> customExcludeUrls) {
        // Check global public endpoints first
        for (String publicPattern : PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(publicPattern, path)) {
                log.debug("Path {} matches public endpoint pattern: {}", path, publicPattern);
                return true;
            }
        }

        // Check custom excluded URLs from configuration
        if (customExcludeUrls != null && !customExcludeUrls.isEmpty()) {
            for (String excludePattern : customExcludeUrls) {
                if (pathMatcher.match(excludePattern, path)) {
                    log.debug("Path {} matches custom exclude pattern: {}", path, excludePattern);
                    return true;
                }
            }
        }

        // Special handling for service-to-service booking endpoints
        // Check if path matches /api/v1/bookings/{any}/update-payment or
        // add-partial-payment
        if (path != null && path.startsWith("/api/v1/bookings/")) {
            // Remove leading /api/v1/bookings/ to get the remaining path
            String remainingPath = path.substring("/api/v1/bookings/".length());
            // Check if it ends with /update-payment or /update-payment/ or
            // /add-partial-payment or /add-partial-payment/
            if (remainingPath.matches(".*/update-payment/?") || remainingPath.matches(".*/add-partial-payment/?")) {
                log.debug("Path {} matches service-to-service booking endpoint pattern", path);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if this is a GET request to blog service endpoints
     * All GET requests to blog service are public (except user-specific endpoints)
     */
    private boolean isBlogGetRequest(String path, String method) {
        if (!"GET".equals(method)) {
            return false;
        }

        // Check if path starts with blog service
        if (!path.startsWith("/api/v1/blog/")) {
            return false;
        }

        // Exclude user-specific endpoints that require authentication
        String[] userSpecificEndpoints = {
                "/api/v1/blog/posts/my-posts",
                "/api/v1/blog/posts/my-likes",
                "/api/v1/blog/comments/my-comments",
                "/api/v1/blog/comments/pending",
                "/api/v1/blog/likes/my-likes",
                "/api/v1/blog/settings/stats"
        };

        for (String userEndpoint : userSpecificEndpoints) {
            if (pathMatcher.match(userEndpoint, path)) {
                log.debug("Path {} is a user-specific blog endpoint, requires authentication", path);
                return false;
            }
        }

        log.debug("Path {} is a GET request to blog service, allowing without authentication", path);
        return true;
    }

    private boolean isAdminOnlyEndpoint(String path) {
        for (String adminPattern : ADMIN_ONLY_ENDPOINTS) {
            if (pathMatcher.match(adminPattern, path)) {
                log.debug("Path {} matches admin-only pattern: {}", path, adminPattern);
                return true;
            }
        }
        return false;
    }

    private boolean isAdmin(GatewayJwtUtil.UserContext userContext) {
        String role = userContext.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
    }

    private boolean isAuthorizedForAdminPath(String path, String method, GatewayJwtUtil.UserContext userContext) {
        if (isAdmin(userContext)) {
            return true;
        }

        if ("EMPLOYEE".equalsIgnoreCase(userContext.getRole())) {
            return isAllowedEmployeeAdminPath(path) && isReadOrCreateOrUpdateMethod(method);
        }

        return false;
    }

    private boolean isAllowedEmployeeAdminPath(String path) {
        return pathMatcher.match("/api/v1/admin/tours/**", path) ||
                pathMatcher.match("/api/v1/admin/destinations/**", path) ||
                pathMatcher.match("/api/v1/admin/activities/**", path) ||
                pathMatcher.match("/api/v1/admin/security/whitelist-my-ip", path) ||
                pathMatcher.match("/api/v1/admin/security/whitelist/me", path);
    }

    private boolean isReadOrCreateOrUpdateMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private boolean hasRequiredPermissions(String path, String method, GatewayJwtUtil.UserContext userContext) {
        // Admin users have access to everything
        if (isAdmin(userContext)) {
            return true;
        }

        // Check payment endpoints - require additional validation
        if (path.startsWith("/api/v1/payments/")) {
            return hasPaymentPermissions(path, method, userContext);
        }

        // Check sensitive user operations
        if (path.startsWith("/api/v1/user/") && isSensitiveUserOperation(path, method)) {
            return hasSensitiveUserPermissions(path, method, userContext);
        }

        // Default: authenticated users can access most endpoints
        return true;
    }

    private boolean isSensitiveUserOperation(String path, String method) {
        // Define sensitive operations that might need additional checks
        List<String> sensitivePaths = Arrays.asList(
                "/api/v1/user/account/delete",
                "/api/v1/user/verification/**",
                "/api/v1/user/documents/**");

        for (String sensitivePath : sensitivePaths) {
            if (pathMatcher.match(sensitivePath, path)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasSensitiveUserPermissions(String path, String method, GatewayJwtUtil.UserContext userContext) {
        // For now, all authenticated users can access sensitive operations
        // In production, you might want to add additional validations like:
        // - Email verification status
        // - Account age
        // - 2FA enabled for sensitive operations
        return true;
    }

    private boolean hasPaymentPermissions(String path, String method, GatewayJwtUtil.UserContext userContext) {
        // For payment operations, ensure user is verified
        // Additional checks can be added here
        return true;
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message, String errorCode) {
        return handleAuthError(exchange, HttpStatus.UNAUTHORIZED, message, errorCode);
    }

    private Mono<Void> handleForbidden(ServerWebExchange exchange, String message, String errorCode) {
        return handleAuthError(exchange, HttpStatus.FORBIDDEN, message, errorCode);
    }

    private Mono<Void> handleAuthError(ServerWebExchange exchange, HttpStatus status, String message,
            String errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        // Use set instead of add to ensure Content-Type is properly set
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().set("X-Content-Type-Options", "nosniff");

        // Add comprehensive CORS headers for browser requests
        addCorsHeaders(response);

        APIResponse<Object> apiResponse = APIResponse.builder()
                .success(false)
                .statusCode(status.value())
                .message(message)
                .data(createErrorData(errorCode, exchange.getRequest().getURI().getPath()))
                .errors(null)
                .build();

        try {
            String responseBody = objectMapper.writeValueAsString(apiResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error writing auth error response: {}", e.getMessage());
            return response.setComplete();
        }
    }

    private void addCorsHeaders(ServerHttpResponse response) {
        // DO NOT add CORS headers here - CorsWebFilter handles all CORS headers
        // Adding headers here causes duplicate Access-Control-Allow-Origin headers
        // CorsWebFilter is the single source of truth for CORS configuration
        // This method is kept for backward compatibility but does nothing
    }

    private Object createErrorData(String errorCode, String path) {
        return new AuthErrorData(
                errorCode,
                path,
                System.currentTimeMillis(),
                getErrorSuggestion(errorCode));
    }

    private String getErrorSuggestion(String errorCode) {
        switch (errorCode) {
            case "TOKEN_MISSING":
                return "Please provide a valid authentication token in the Authorization header";
            case "TOKEN_INVALID":
                return "Your session has expired. Please login again";
            case "TOKEN_MALFORMED":
                return "Invalid token format. Please login again";
            case "INSUFFICIENT_ADMIN_PERMISSIONS":
                return "This operation requires administrator privileges";
            case "INSUFFICIENT_PERMISSIONS":
                return "You don't have permission to perform this operation";
            default:
                return "Please ensure you are properly authenticated";
        }
    }

    public static class Config {
        private List<String> excludeUrls;

        public List<String> getExcludeUrls() {
            return excludeUrls;
        }

        public void setExcludeUrls(List<String> excludeUrls) {
            this.excludeUrls = excludeUrls;
        }
    }

    public static class AuthErrorData {
        public final String errorCode;
        public final String requestPath;
        public final long timestamp;
        public final String suggestion;

        public AuthErrorData(String errorCode, String requestPath, long timestamp, String suggestion) {
            this.errorCode = errorCode;
            this.requestPath = requestPath;
            this.timestamp = timestamp;
            this.suggestion = suggestion;
        }
    }
}
