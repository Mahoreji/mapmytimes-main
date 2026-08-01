package in.mapmytour.api.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Request/Response Logging Filter for API Gateway
 * Logs request and response details with correlation IDs
 */
@Component
public class RequestResponseLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip WebSocket/streaming and OAuth2 redirection endpoints
        if (path.startsWith("/auth/ws/") || path.equals("/auth/ws") || 
            path.contains("/oauth2/") || path.contains("/login/oauth2/")) {
            return chain.filter(exchange);
        }

        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);

        // Log request
        logRequest(request, correlationId, traceId);

        // Track if response has already been logged for this exchange
        // (Fail-safe against multiple beforeCommit triggers if any)
        exchange.getResponse().beforeCommit(() -> Mono.fromRunnable(() -> {
            int statusCode = (exchange.getResponse().getStatusCode() != null) 
                             ? exchange.getResponse().getStatusCode().value() 
                             : 200;
            logResponse(exchange.getRequest(), statusCode, correlationId, traceId);
        }));

        return chain.filter(exchange);
    }

    private void logRequest(ServerHttpRequest request, String correlationId, String traceId) {
        if (log.isDebugEnabled()) {
            // Redact sensitive headers
            String authHeader = redactAuthorizationHeader(request.getHeaders().getFirst("Authorization"));
            String cookieHeader = redactHeader(request.getHeaders().getFirst("Cookie"));
            String setCookieHeader = redactHeader(request.getHeaders().getFirst("Set-Cookie"));
            String apiKeyHeader = redactHeader(request.getHeaders().getFirst("X-Api-Key"));

            log.debug("Request: method={}, path={}, query={}, correlationId={}, traceId={}, remoteAddr={}, " +
                    "authorization={}, cookie={}, setCookie={}, apiKey={}",
                    request.getMethod(), request.getPath(), request.getQueryParams(),
                    correlationId, traceId, request.getRemoteAddress(),
                    authHeader, cookieHeader, setCookieHeader, apiKeyHeader);
        } else if (log.isInfoEnabled()) {
            // Info level: minimal logging without sensitive data
            log.info("Request: method={}, path={}, correlationId={}, traceId={}",
                    request.getMethod(), request.getPath(), correlationId, traceId);
        }
    }

    /**
     * Redact Authorization header - keep only "Bearer ***"
     */
    private String redactAuthorizationHeader(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return "Bearer ***";
        }
        if (authHeader.startsWith("Basic ")) {
            return "Basic ***";
        }
        return "***";
    }

    /**
     * Redact sensitive header values
     */
    private String redactHeader(String headerValue) {
        return headerValue != null ? "***" : null;
    }

    private void logResponse(ServerHttpRequest request, int statusCode, String correlationId, String traceId) {
        if (log.isDebugEnabled()) {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("Response: status=").append(statusCode)
                    .append(", path=").append(request.getPath())
                    .append(", correlationId=").append(correlationId)
                    .append(", traceId=").append(traceId);

            // Redact sensitive response headers
            String setCookieHeader = redactHeader(request.getHeaders().getFirst("Set-Cookie"));
            if (setCookieHeader != null) {
                logMessage.append(", setCookie=").append(setCookieHeader);
            }

            log.debug(logMessage.toString());
        } else if (log.isInfoEnabled()) {
            log.info("Response: status={}, path={}, correlationId={}, traceId={}",
                    statusCode, request.getPath(), correlationId, traceId);
        }
    }

    @Override
    public int getOrder() {
        return -99; // After CorrelationIdFilter but before other filters
    }
}
