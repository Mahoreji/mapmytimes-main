package in.mapmytour.auth.config;

import in.mapmytour.auth.utils.SignatureUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to ensure ALL endpoints are only accessible through API Gateway.
 * Blocks direct access to auth service endpoints by verifying HMAC signature.
 */
@Component
@Order(0) // Execute before all other filters
@Slf4j
public class GatewayOnlyAccessFilter extends OncePerRequestFilter {

    private static final String GATEWAY_HEADER = "X-Request-Source";
    private static final String GATEWAY_HEADER_VALUE = "api-gateway";
    private static final String SIGNATURE_HEADER = "X-Gateway-Signature";

    private final SignatureUtils signatureUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.gateway-only.enabled:true}")
    private boolean gatewayOnlyEnabled;

    public GatewayOnlyAccessFilter(SignatureUtils signatureUtils) {
        this.signatureUtils = signatureUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // If gateway-only mode is disabled, allow all requests
        if (!gatewayOnlyEnabled) {
            log.debug("Gateway-only mode is disabled, allowing request: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        // Extract headers for signature verification
        String requestSource = request.getHeader(GATEWAY_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);

        // Map for signature calculation
        Map<String, String> headersToVerify = new HashMap<>();
        String[] headersToSign = {
            "X-User-Id", "X-User-Email", "X-User-Role", 
            "X-Authenticated", "X-Request-Source", "X-Gateway-Timestamp"
        };
        
        for (String header : headersToSign) {
            String value = request.getHeader(header);
            if (value != null) {
                headersToVerify.put(header, value);
            }
        }

        boolean isValidSignature = signatureUtils.verifySignature(headersToVerify, signature);
        boolean isFromGateway = GATEWAY_HEADER_VALUE.equalsIgnoreCase(requestSource);
        boolean isInternalCall = "internal-service".equalsIgnoreCase(requestSource);

        if (isFromGateway || isInternalCall) {
            if (!isValidSignature) {
                log.warn("Invalid signature for {}: {} {} from IP: {}",
                        requestSource, method, path, getClientIpAddress(request));
                sendForbiddenResponse(response, path, "Invalid request signature for " + requestSource);
                return;
            }
        } else {
            log.warn("Direct access attempt blocked: {} {} from IP: {}",
                    method, path, getClientIpAddress(request));
            sendForbiddenResponse(response, path, "Direct access to this service is NOT allowed. Access must be via API Gateway or internal trust.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // health checks are fine to bypass if accessed directly from infra
        if (path.equals("/health") || path.equals("/api/v1/auth/health")) {
            return true;
        }

        // WebSocket endpoints - SockJS needs /ws/info for handshake
        // WebSocket connections are handled separately and don't go through gateway
        if (path.startsWith("/ws/") || path.equals("/ws") || 
            path.startsWith("/api/v1/auth/ws/") || path.equals("/api/v1/auth/ws")) {
            return true;
        }

        return false;
    }

    private void sendForbiddenResponse(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("statusCode", HttpStatus.FORBIDDEN.value());
        errorResponse.put("message", message);
        errorResponse.put("errorCode", "DIRECT_ACCESS_FORBIDDEN");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", path);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
