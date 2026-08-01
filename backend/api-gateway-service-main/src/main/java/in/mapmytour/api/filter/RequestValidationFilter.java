package in.mapmytour.api.filter;

import in.mapmytour.api.service.SecurityLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * RequestValidationFilter — Firewall that inspects the raw HTTP request and
 * rejects anything that is malformed, oversized, or contains suspicious patterns
 * before it touches any backend microservice.
 *
 * Checks performed:
 *   1. Oversized Content-Length (configurable max body size)
 *   2. Content-Type validation (refuse unknown / wrong types for mutating methods)
 *   3. Malformed / excessively large header values
 *   4. Suspicious query-string patterns (SQLi, path traversal, null-bytes)
 *   5. HTTP header injection (CR/LF sequences)
 *   6. Reject requests with invalid characters in the path
 *
 * Filter order: -50 — runs after ThreatScoring (-100) and before
 * AuthenticationFilter (which is a route-level GatewayFilter).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RequestValidationFilter implements GlobalFilter, Ordered {

    private final SecurityLoggingService loggingService;

    @Value("${security.validation.max-content-length-bytes:209715200}") // 200 MB default
    private long maxContentLength;

    @Value("${security.validation.max-header-value-length:8192}")
    private int maxHeaderValueLength;

    // ── Allowed Content-Types for mutating requests ───────────────────────
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            "application/x-www-form-urlencoded",
            "text/plain"
    );

    // ── SQLi / traversal probes in paths and query strings ────────────────
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)(union\\s+select|drop\\s+table|insert\\s+into|exec\\s*\\()"),
            Pattern.compile("\\.{2}[/\\\\]"),                      // path traversal  ../
            Pattern.compile("%00"),                                 // null-byte
            Pattern.compile("(?i)(%0d|%0a|\\r|\\n)"),             // CRLF injection
            Pattern.compile("(?i)(<script|javascript:|data:text)") // XSS seeds
    );

    // ── Valid HTTP methods ────────────────────────────────────────────────
    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD");

    // ── Mutating methods that should carry a proper Content-Type ─────────
    private static final List<String> MUTATING_METHODS = List.of("POST", "PUT", "PATCH");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path   = request.getURI().getPath();
        String method = request.getMethod().name();
        String clientIp = resolveClientIp(exchange);

        // ─── 1. Validate HTTP method ───────────────────────────────────────
        if (!ALLOWED_METHODS.contains(method.toUpperCase())) {
            return reject(exchange, clientIp, path, method, "Invalid HTTP method: " + method,
                    HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED");
        }

        // ─── 2. Payload size guard ─────────────────────────────────────────
        long contentLength = request.getHeaders().getContentLength();
        if (contentLength > maxContentLength) {
            return reject(exchange, clientIp, path, method,
                    "Content-Length " + contentLength + " exceeds max " + maxContentLength,
                    HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE");
        }

        // ─── 3. Content-Type validation for mutating methods ──────────────
        if (MUTATING_METHODS.contains(method.toUpperCase()) && contentLength > 0) {
            MediaType ct = request.getHeaders().getContentType();
            if (ct != null && !isAllowedContentType(ct)) {
                return reject(exchange, clientIp, path, method,
                        "Unsupported Content-Type: " + ct,
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_CONTENT_TYPE");
            }
        }

        // ─── 4. Header value length and injection guard ───────────────────
        for (var entry : request.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                if (value != null && value.length() > maxHeaderValueLength) {
                    return reject(exchange, clientIp, path, method,
                            "Header value too long: " + entry.getKey(),
                            HttpStatus.BAD_REQUEST, "HEADER_TOO_LARGE");
                }
                if (value != null && containsCrLf(value)) {
                    return reject(exchange, clientIp, path, method,
                            "Header injection attempt in: " + entry.getKey(),
                            HttpStatus.BAD_REQUEST, "HEADER_INJECTION");
                }
            }
        }

        // ─── 5. Path injection check ──────────────────────────────────────
        if (hasInjection(path)) {
            return reject(exchange, clientIp, path, method,
                    "Suspicious pattern in request path",
                    HttpStatus.BAD_REQUEST, "INVALID_PATH");
        }

        // ─── 6. Query string injection check ─────────────────────────────
        String query = request.getURI().getQuery();
        if (query != null && hasInjection(query)) {
            return reject(exchange, clientIp, path, method,
                    "Suspicious pattern in query string",
                    HttpStatus.BAD_REQUEST, "INVALID_QUERY");
        }

        return chain.filter(exchange);
    }

    // =====================================================================
    // Validation Helpers
    // =====================================================================

    private boolean isAllowedContentType(MediaType ct) {
        return ALLOWED_CONTENT_TYPES.stream().anyMatch(allowed -> {
            try {
                return ct.isCompatibleWith(MediaType.parseMediaType(allowed));
            } catch (Exception e) {
                return false;
            }
        });
    }

    private boolean containsCrLf(String value) {
        return value.contains("\r") || value.contains("\n")
                || value.contains("%0d") || value.contains("%0a")
                || value.contains("%0D") || value.contains("%0A");
    }

    private boolean hasInjection(String input) {
        return INJECTION_PATTERNS.stream().anyMatch(p -> p.matcher(input).find());
    }

    // =====================================================================
    // Response Helpers
    // =====================================================================

    private Mono<Void> reject(ServerWebExchange exchange, String clientIp,
                              String path, String method, String reason,
                              HttpStatus status, String errorCode) {
        loggingService.logBlockedRequest(clientIp, path, method, reason, 0);
        log.warn("Request rejected [{}]: ip={} path={} reason={}", errorCode, clientIp, path, reason);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String body = String.format("{\"success\":false,\"statusCode\":%d,\"message\":\"%s\",\"errorCode\":\"%s\"}",
                status.value(), status.getReasonPhrase(), errorCode);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        var headers = exchange.getRequest().getHeaders();
        String xff = headers.getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String realIp = headers.getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
