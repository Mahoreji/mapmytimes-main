package in.mapmytour.blog.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter for Blog Service
 * Extracts correlation ID from headers and adds to MDC for logging
 */
@Component
@Order(1)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String correlationId = getOrGenerateCorrelationId(request);
            String traceId = getOrGenerateTraceId(request);
            String spanId = getOrGenerateSpanId(request);
            
            MDC.put("correlationId", correlationId);
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
            
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            
            request.setAttribute(CORRELATION_ID_HEADER, correlationId);
            request.setAttribute(TRACE_ID_HEADER, traceId);
            request.setAttribute(SPAN_ID_HEADER, spanId);
            
            log.debug("Processing request with correlationId={}, traceId={}, spanId={}", 
                    correlationId, traceId, spanId);
            
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String getOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String getOrGenerateSpanId(HttpServletRequest request) {
        String spanId = request.getHeader(SPAN_ID_HEADER);
        if (spanId == null || spanId.isEmpty()) {
            spanId = UUID.randomUUID().toString().substring(0, 16);
        }
        return spanId;
    }
}
