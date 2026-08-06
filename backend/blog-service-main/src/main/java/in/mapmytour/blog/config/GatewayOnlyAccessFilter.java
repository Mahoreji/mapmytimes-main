package in.mapmytour.blog.config;

import in.mapmytour.blog.utils.SignatureUtils;
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
 * Blocks direct access to blog service endpoints by verifying HMAC signature.
 * Pattern matched from hotel-services.
 */
@Component
@Order(1) // Execute before other filters
@Slf4j
public class GatewayOnlyAccessFilter extends OncePerRequestFilter {

    private static final String GATEWAY_HEADER = "X-Request-Source";
    private static final String GATEWAY_HEADER_VALUE = "api-gateway";
    private static final String SIGNATURE_HEADER = "X-Gateway-Signature";

    @Value("${app.security.gateway-only.enabled:true}")
    private boolean gatewayOnlyEnabled;

    @Value("${jwt.secret.key}")
    private String gatewaySecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Check for gateway header - ALL requests must come through gateway
        String requestSource = request.getHeader(GATEWAY_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);
        
        boolean isFromGateway = GATEWAY_HEADER_VALUE.equalsIgnoreCase(requestSource);

        if (!isFromGateway || signature == null) {
            log.warn("Direct access attempt blocked: {} {} from IP: {}. Missing gateway headers.",
                    method, path, getClientIpAddress(request));
            sendForbiddenResponse(response, path, "Direct access is forbidden. Missing security signatures.");
            return;
        }

        // Verify HMAC signature
        try {
            Map<String, String> signHeaders = new HashMap<>();
            String[] headersToSign = {
                "X-User-Id", "X-User-Email", "X-User-Role", 
                "X-Authenticated", "X-Request-Source", "X-Gateway-Timestamp"
            };
            
            for (String header : headersToSign) {
                String value = request.getHeader(header);
                if (value != null) {
                    signHeaders.put(header, value);
                }
            }

            if (!SignatureUtils.verifySignature(signHeaders, signature, gatewaySecret)) {
                log.error("Invalid Gateway Signature detected for path: {}", path);
                sendForbiddenResponse(response, path, "Invalid security signature.");
                return;
            }
        } catch (Exception e) {
            log.error("Error verifying gateway signature: {}", e.getMessage());
            sendForbiddenResponse(response, path, "Security verification failed.");
            return;
        }

        log.debug("Request from API Gateway allowed: {} {}", method, path);
        filterChain.doFilter(request, response);
    }


    private void sendForbiddenResponse(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("statusCode", HttpStatus.FORBIDDEN.value());
        errorResponse.put("message", message);
        errorResponse.put("errorCode", "GATEWAY_VERIFICATION_FAILED");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", path);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (path.equals("/health") || path.equals("/api/v1/blog/health") || path.equals("/actuator/health") || path.equals("/actuator/info")) {
            return true;
        }

        if ("/api/v1/blog/translate".equals(path)) {
            return true;
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && (remoteAddr.equals("127.0.0.1") || remoteAddr.equals("0:0:0:0:0:0:0:1") || remoteAddr.equals("::1"))) {
            return true;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (firstIp.equals("127.0.0.1") || firstIp.equals("0:0:0:0:0:0:0:1") || firstIp.equals("::1")) {
                return true;
            }
        }

        return false;
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
