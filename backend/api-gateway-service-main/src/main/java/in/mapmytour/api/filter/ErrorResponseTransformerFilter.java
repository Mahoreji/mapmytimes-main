package in.mapmytour.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.mapmytour.api.dto.APIResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Filter to transform HTML error responses from downstream services to JSON
 * This ensures all error responses are JSON, never HTML
 * Runs early in the filter chain to catch all error responses
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorResponseTransformerFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(ErrorResponseTransformerFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // Skip WebSocket/streaming endpoints - buffering breaks SockJS streaming
        if (path.startsWith("/auth/ws/") || path.equals("/auth/ws")) {
            return chain.filter(exchange);
        }
        
        ServerHttpResponse originalResponse = exchange.getResponse();
        
        // Create final references for use in lambda expressions
        final ServerWebExchange finalExchange = exchange;
        
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                org.springframework.http.HttpStatusCode statusCodeObj = getStatusCode();
                final HttpStatus statusCode;
                if (statusCodeObj != null) {
                    int statusValue = statusCodeObj.value();
                    statusCode = HttpStatus.resolve(statusValue);
                } else {
                    statusCode = null;
                }
                
                // Only process error responses (4xx, 5xx)
                if (statusCode != null && statusCode.isError()) {
                    MediaType contentType = getHeaders().getContentType();
                    
                    // Optimization: If it's already JSON or another non-HTML type, pass through immediately
                    // This avoids loading the entire body into memory unnecessarily
                    if (contentType != null && !contentType.includes(MediaType.TEXT_HTML) && 
                        !contentType.includes(MediaType.APPLICATION_XHTML_XML) &&
                        !contentType.isCompatibleWith(MediaType.ALL)) {
                        return super.writeWith(body);
                    }

                    return DataBufferUtils.join(Flux.from(body))
                        .flatMap(dataBuffer -> {
                            // Read the response body
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            DataBufferUtils.release(dataBuffer);
                            
                            String responseBody = new String(content, StandardCharsets.UTF_8);
                            
                            // Check if response is HTML error page
                            if (isHtmlErrorPage(responseBody)) {
                                log.warn("Detected HTML error response ({}), transforming to JSON. Path: {}", 
                                    statusCode.value(), finalExchange.getRequest().getURI().getPath());
                                
                                return transformToJsonError(finalExchange, statusCode, responseBody);
                            }
                            
                            // Check if Content-Type was HTML but check above was inconclusive
                            if (contentType != null && contentType.includes(MediaType.TEXT_HTML)) {
                                log.warn("Detected HTML Content-Type in error response ({}), transforming to JSON. Path: {}", 
                                    statusCode.value(), finalExchange.getRequest().getURI().getPath());
                                
                                return transformToJsonError(finalExchange, statusCode, responseBody);
                            }
                            
                            // Check if response body is empty but status is error - might be HTML error page from some servers
                            if (responseBody.isEmpty() && statusCode.isError()) {
                                log.debug("Empty error response body detected ({}) for path: {}", 
                                    statusCode.value(), finalExchange.getRequest().getURI().getPath());
                                return transformToJsonError(finalExchange, statusCode, null);
                            }
                            
                            // Return original response if not HTML
                            DataBuffer buffer = bufferFactory().wrap(content);
                            return super.writeWith(Mono.just(buffer));
                        })
                        .onErrorResume(ex -> {
                            log.error("Error processing response body", ex);
                            final HttpStatus errorStatus = statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR;
                            return transformToJsonError(finalExchange, errorStatus, null);
                        });
                }
                
                // For non-error responses, pass through
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private boolean isHtmlErrorPage(String body) {
        if (body == null || body.isEmpty()) {
            return false;
        }
        String lowerBody = body.toLowerCase();
        return lowerBody.contains("<!doctype") || 
               lowerBody.contains("<html") || 
               lowerBody.contains("<title>") ||
               lowerBody.contains("http status");
    }

    private Mono<Void> transformToJsonError(ServerWebExchange exchange, HttpStatus status, String htmlBody) {
        ServerHttpResponse response = exchange.getResponse();
        
        // Ensure response is not already committed
        if (response.isCommitted()) {
            log.warn("Response already committed, cannot transform error");
            return Mono.empty();
        }
        
        // Set JSON content type
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("X-Content-Type-Options", "nosniff");
        response.setStatusCode(status);
        
        // Add CORS headers
        addCorsHeaders(response);
        
        // Extract error message from HTML if possible
        String errorMessage = extractErrorMessageFromHtml(htmlBody, status);
        
        // Create JSON error response
        APIResponse<Object> apiResponse = APIResponse.builder()
                .success(false)
                .statusCode(status.value())
                .message(errorMessage)
                .data(createErrorData(status, exchange.getRequest().getURI().getPath(), 
                    exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN"))
                .errors(null)
                .build();

        try {
            String jsonResponse = objectMapper.writeValueAsString(apiResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            log.debug("Transformed HTML error to JSON: {} - {}", status.value(), errorMessage);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error transforming HTML error to JSON", e);
            return response.setComplete();
        }
    }
    
    @SuppressWarnings("unused")
    private void addCorsHeaders(ServerHttpResponse response) {
        // DO NOT add CORS headers here - CorsWebFilter handles all CORS headers
        // Adding headers here causes duplicate Access-Control-Allow-Origin headers
        // CorsWebFilter is the single source of truth for CORS configuration
        // This method is kept for backward compatibility but does nothing
    }

    private String extractErrorMessageFromHtml(String htmlBody, HttpStatus status) {
        // Try to extract meaningful error message from HTML
        if (htmlBody.contains("Bad Request")) {
            return "Bad Request - Invalid request format or missing required parameters.";
        }
        if (htmlBody.contains("Unauthorized")) {
            return "Unauthorized - Authentication required.";
        }
        if (htmlBody.contains("Forbidden")) {
            return "Forbidden - Insufficient permissions.";
        }
        if (htmlBody.contains("Not Found")) {
            return "Not Found - The requested resource was not found.";
        }
        
        // Default message based on status code
        switch (status) {
            case BAD_REQUEST:
                return "Bad Request - Invalid request format or missing required parameters.";
            case UNAUTHORIZED:
                return "Unauthorized - Authentication required.";
            case FORBIDDEN:
                return "Forbidden - Insufficient permissions.";
            case NOT_FOUND:
                return "Not Found - The requested resource was not found.";
            case INTERNAL_SERVER_ERROR:
                return "Internal Server Error - An unexpected error occurred.";
            default:
                return status.getReasonPhrase();
        }
    }

    private Object createErrorData(HttpStatus status, String path, String method) {
        return new ErrorData(
                "HTTP_" + status.value(),
                path,
                method,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN)),
                getErrorSuggestion(status)
        );
    }

    private String getErrorSuggestion(HttpStatus status) {
        switch (status) {
            case BAD_REQUEST:
                return "Please check your request format, required parameters, and ensure Content-Type is application/json.";
            case UNAUTHORIZED:
                return "Please provide a valid authentication token in the Authorization header.";
            case FORBIDDEN:
                return "You don't have permission to access this resource.";
            case NOT_FOUND:
                return "Please verify the request URL and try again.";
            case SERVICE_UNAVAILABLE:
                return "The service is temporarily unavailable. Please try again later.";
            default:
                return "Please try again. If the problem persists, contact support.";
        }
    }

    public static class ErrorData {
        public final String errorCode;
        public final String requestPath;
        public final String requestMethod;
        public final String timestamp;
        public final String suggestion;

        public ErrorData(String errorCode, String requestPath, String requestMethod,
                         String timestamp, String suggestion) {
            this.errorCode = errorCode;
            this.requestPath = requestPath;
            this.requestMethod = requestMethod;
            this.timestamp = timestamp;
            this.suggestion = suggestion;
        }
    }
}
