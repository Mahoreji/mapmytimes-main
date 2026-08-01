package in.mapmytour.api.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Correlation ID Filter
 * Generates and propagates correlation IDs across all services for distributed tracing
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Get or generate correlation ID
        String correlationId = getOrGenerateCorrelationId(request);
        String traceId = getOrGenerateTraceId(request);
        String spanId = generateSpanId();
        
        // Add to request headers for downstream services
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(TRACE_ID_HEADER, traceId)
                .header(SPAN_ID_HEADER, spanId)
                .build();
        
        // Add to response headers for client (use set to prevent duplicates)
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set(CORRELATION_ID_HEADER, correlationId);
        response.getHeaders().set(TRACE_ID_HEADER, traceId);
        
        // Log request with correlation ID
        log.info("Request correlationId={}, traceId={}, spanId={}, path={}", 
                correlationId, traceId, spanId, request.getPath());
        
        // Create modified exchange with updated request
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();
        
        return chain.filter(modifiedExchange)
                .doFinally(signalType -> {
                    log.info("Response correlationId={}, status={}", 
                            correlationId, response.getStatusCode());
                });
    }

    private String getOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String getOrGenerateTraceId(ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String generateSpanId() {
        return UUID.randomUUID().toString().substring(0, 16);
    }

    @Override
    public int getOrder() {
        return -100; // Execute early, before other filters
    }
}
