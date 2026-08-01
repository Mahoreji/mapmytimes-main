package in.mapmytour.auth.security;

import in.mapmytour.auth.service.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        // Skip OAuth2 endpoints
        if (endpoint.startsWith("/oauth2/") || endpoint.startsWith("/login/oauth2/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Apply rate limiting to sensitive endpoints
        if (isSensitiveEndpoint(endpoint, method)) {
            String rateLimitKey = clientIp + ":" + endpoint + ":" + method;
            int maxAttempts = getRateLimit(endpoint);
            Duration timeWindow = Duration.ofMinutes(15);

            if (rateLimitingService.isRateLimited(rateLimitKey, maxAttempts, timeWindow)) {
                log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, endpoint);
                sendRateLimitResponse(response, maxAttempts, timeWindow);
                return;
            }

            rateLimitingService.recordAttempt(rateLimitKey);

            // Add rate limit headers
            int remaining = rateLimitingService.getRemainingAttempts(rateLimitKey, maxAttempts, timeWindow);
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(maxAttempts));
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
            response.setHeader("X-Rate-Limit-Reset", String.valueOf(System.currentTimeMillis() + timeWindow.toMillis()));
        }

        // Check if IP is temporarily blocked
        if (rateLimitingService.isBlocked(clientIp)) {
            log.warn("Blocked IP attempted access: {} on endpoint: {}", clientIp, endpoint);
            sendBlockedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSensitiveEndpoint(String endpoint, String method) {
        return (endpoint.contains("/login") && "POST".equals(method)) ||
                (endpoint.contains("/register") && "POST".equals(method)) ||
                (endpoint.contains("/forgot-password") && "POST".equals(method)) ||
                (endpoint.contains("/reset-password") && "POST".equals(method)) ||
                (endpoint.contains("/verify-email") && "POST".equals(method)) ||
                (endpoint.contains("/resend-verification") && "POST".equals(method));
    }

    private int getRateLimit(String endpoint) {
        if (endpoint.contains("/login")) return 5;
        if (endpoint.contains("/register")) return 3;
        if (endpoint.contains("/forgot-password")) return 3;
        if (endpoint.contains("/verify-email")) return 5;
        if (endpoint.contains("/resend-verification")) return 3;
        return 10;
    }

    private void sendRateLimitResponse(HttpServletResponse response, int maxAttempts, Duration timeWindow) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("statusCode", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("message", "Rate limit exceeded. Too many requests.");
        errorResponse.put("error", "RATE_LIMIT_EXCEEDED");
        errorResponse.put("retryAfter", timeWindow.toSeconds());
        errorResponse.put("maxAttempts", maxAttempts);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private void sendBlockedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("statusCode", HttpStatus.FORBIDDEN.value());
        errorResponse.put("message", "Access blocked due to suspicious activity.");
        errorResponse.put("error", "IP_BLOCKED");

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
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
}