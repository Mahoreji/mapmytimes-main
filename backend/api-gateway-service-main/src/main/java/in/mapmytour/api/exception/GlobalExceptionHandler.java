package in.mapmytour.api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.mapmytour.api.dto.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@Order(-2) // Higher priority to catch errors before default handlers
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            log.warn("Response already committed, cannot handle exception: {}", ex.getMessage());
            return Mono.error(ex);
        }

        // Always set JSON content type first to prevent HTML error pages
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("X-Content-Type-Options", "nosniff");
        addCorsHeaders(response);

        // Log the error for debugging
        log.error("GlobalExceptionHandler caught error: {} - Path: {} - Message: {}",
                ex.getClass().getSimpleName(),
                exchange.getRequest().getURI().getPath(),
                ex.getMessage());

        APIResponse<Object> apiResponse;
        HttpStatus status;
        String errorCode;

        // Handle different types of exceptions
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            status = (HttpStatus) responseStatusException.getStatusCode();
            errorCode = "HTTP_" + status.value();
            String reason = responseStatusException.getReason();
            // Handle 400 Bad Request specifically
            if (status == HttpStatus.BAD_REQUEST) {
                apiResponse = createErrorResponse(status,
                        reason != null ? reason
                                : "Bad Request - Invalid request format or missing required parameters.",
                        "BAD_REQUEST", exchange, ex);
            } else {
                apiResponse = createErrorResponse(status, reason, errorCode, exchange, ex);
            }

        } else if (ex instanceof org.springframework.cloud.gateway.support.TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            errorCode = "GATEWAY_TIMEOUT";
            apiResponse = createErrorResponse(status,
                    "The service took too long to respond. Please try again.", errorCode, exchange, ex);

        } else if (ex instanceof org.springframework.web.server.ServerWebInputException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "INVALID_REQUEST";
            String message = ex.getMessage();
            if (message != null && message.contains("Required request body is missing")) {
                apiResponse = createErrorResponse(status,
                        "Request body is required but was missing. Please provide the required data in JSON format.",
                        errorCode, exchange, ex);
            } else if (message != null && message.contains("JSON parse error")) {
                apiResponse = createErrorResponse(status,
                        "Invalid JSON format. Please check your request body syntax.", errorCode, exchange, ex);
            } else {
                apiResponse = createErrorResponse(status,
                        message != null ? message : "Invalid request format or missing required parameters.", errorCode,
                        exchange, ex);
            }

        } else if (ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            errorCode = "SERVICE_NOT_FOUND";
            apiResponse = createErrorResponse(status,
                    "The requested service endpoint was not found.", errorCode, exchange, ex);

        } else if (ex instanceof java.net.ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "SERVICE_UNAVAILABLE";
            apiResponse = createErrorResponse(status,
                    "Unable to connect to the service. Please try again later.", errorCode, exchange, ex);

        } else if (ex instanceof java.net.SocketTimeoutException) {
            status = HttpStatus.REQUEST_TIMEOUT;
            errorCode = "REQUEST_TIMEOUT";
            apiResponse = createErrorResponse(status,
                    "Request timed out. Please try again with a smaller request.", errorCode, exchange, ex);

        } else if (ex instanceof io.netty.channel.ConnectTimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            errorCode = "CONNECTION_TIMEOUT";
            apiResponse = createErrorResponse(status,
                    "Connection to service timed out. Service may be temporarily unavailable.", errorCode, exchange,
                    ex);

        } else if (ex instanceof org.springframework.web.server.MethodNotAllowedException) {
            status = HttpStatus.METHOD_NOT_ALLOWED;
            errorCode = "METHOD_NOT_ALLOWED";
            apiResponse = createErrorResponse(status,
                    "The HTTP method is not allowed for this endpoint.", errorCode, exchange, ex);

        } else if (ex instanceof org.springframework.web.server.UnsupportedMediaTypeStatusException) {
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            errorCode = "UNSUPPORTED_MEDIA_TYPE";
            apiResponse = createErrorResponse(status,
                    "Unsupported media type. Please check Content-Type header.", errorCode, exchange, ex);

        } else if (ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "SERVICE_CONNECTION_REFUSED";
            apiResponse = createErrorResponse(status,
                    "Service is temporarily unavailable. Connection was refused.", errorCode, exchange, ex);

        } else if (ex.getMessage() != null && ex.getMessage().contains("No route found")) {
            status = HttpStatus.NOT_FOUND;
            errorCode = "ROUTE_NOT_FOUND";
            apiResponse = createErrorResponse(status,
                    "No route configured for this request path.", errorCode, exchange, ex);

        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "INVALID_ARGUMENT";
            apiResponse = createErrorResponse(status,
                    ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided in the request.", errorCode,
                    exchange, ex);

        } else if (ex instanceof org.springframework.web.bind.support.WebExchangeBindException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "VALIDATION_ERROR";
            org.springframework.web.bind.support.WebExchangeBindException bindException = (org.springframework.web.bind.support.WebExchangeBindException) ex;
            StringBuilder errorMessages = new StringBuilder("Validation failed: ");
            bindException.getBindingResult().getFieldErrors().forEach(error -> {
                errorMessages.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
            });
            apiResponse = createErrorResponse(status, errorMessages.toString(), errorCode, exchange, ex);

        } else if (ex instanceof org.springframework.web.server.ServerErrorException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "SERVER_ERROR";
            apiResponse = createErrorResponse(status,
                    ex.getMessage() != null ? ex.getMessage() : "Server error occurred.", errorCode, exchange, ex);

        } else if (ex.getMessage() != null
                && (ex.getMessage().contains("400") || ex.getMessage().contains("Bad Request"))) {
            // Catch any error message containing 400 or Bad Request
            status = HttpStatus.BAD_REQUEST;
            errorCode = "BAD_REQUEST";
            String cleanMessage = ex.getMessage();
            // Remove HTML tags if present
            cleanMessage = cleanMessage.replaceAll("<[^>]*>", "").trim();
            if (cleanMessage.isEmpty() || cleanMessage.equals("400") || cleanMessage.equals("Bad Request")) {
                cleanMessage = "Bad Request - Invalid request format or missing required parameters.";
            }
            apiResponse = createErrorResponse(status, cleanMessage, errorCode, exchange, ex);

        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_SERVER_ERROR";
            apiResponse = createErrorResponse(status,
                    "An unexpected error occurred. Our team has been notified.", errorCode, exchange, ex);
            log.error("Unhandled exception in gateway", ex);
        }

        response.setStatusCode(status);

        try {
            String responseBody = objectMapper.writeValueAsString(apiResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error writing exception response", e);
            return response.setComplete();
        }
    }

    private APIResponse<Object> createErrorResponse(HttpStatus status, String message, String errorCode,
            ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        return APIResponse.builder()
                .success(false)
                .statusCode(status.value())
                .message(message != null ? message : status.getReasonPhrase())
                .data(createErrorData(errorCode, path, method, ex))
                .errors(createErrorList(status, ex))
                .build();
    }

    private Object createErrorData(String errorCode, String path, String method, Throwable ex) {
        return new ErrorData(
                errorCode,
                path,
                method,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN)),
                getErrorSuggestion(errorCode),
                ex.getClass().getSimpleName());
    }

    private List<String> createErrorList(HttpStatus status, Throwable ex) {
        if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            String message = ex.getMessage();
            
            // SECURITY: Don't leak implementation details in internal server errors
            // If the status is 500, always hide the detailed message
            if (status == null || status == HttpStatus.INTERNAL_SERVER_ERROR) {
                return Arrays.asList("An internal server error occurred. Please contact support if the problem persists.");
            }

            // For other errors (like 400 Bad Request), show the message but filter out HTML
            if (message.contains("<html>") || message.contains("<!doctype") || message.contains("<title>")) {
                return null; 
            }
            return Arrays.asList(message);
        }
        return null;
    }

    private String getErrorSuggestion(String errorCode) {
        switch (errorCode) {
            case "GATEWAY_TIMEOUT":
            case "CONNECTION_TIMEOUT":
                return "The service is taking longer than usual. Please wait a moment and try again.";
            case "SERVICE_UNAVAILABLE":
            case "SERVICE_CONNECTION_REFUSED":
                return "The service is temporarily unavailable. Please try again in a few minutes.";
            case "SERVICE_NOT_FOUND":
            case "ROUTE_NOT_FOUND":
                return "Please verify the request URL and try again.";
            case "INVALID_REQUEST":
            case "INVALID_ARGUMENT":
                return "Please check your request format and required parameters.";
            case "METHOD_NOT_ALLOWED":
                return "Please check the HTTP method used for this endpoint.";
            case "UNSUPPORTED_MEDIA_TYPE":
                return "Please ensure you're sending data in the correct format (JSON).";
            case "REQUEST_TIMEOUT":
                return "Request took too long to process. Please try with a smaller request.";
            default:
                return "Please try again. If the problem persists, contact support.";
        }
    }

    private void addCorsHeaders(ServerHttpResponse response) {
        // DO NOT add CORS headers here - CorsWebFilter handles all CORS headers
        // Adding headers here causes duplicate Access-Control-Allow-Origin headers
        // CorsWebFilter is the single source of truth for CORS configuration
        // This method is kept for backward compatibility but does nothing
    }

    public static class ErrorData {
        public final String errorCode;
        public final String requestPath;
        public final String requestMethod;
        public final String timestamp;
        public final String suggestion;
        public final String exceptionType;

        public ErrorData(String errorCode, String requestPath, String requestMethod,
                String timestamp, String suggestion, String exceptionType) {
            this.errorCode = errorCode;
            this.requestPath = requestPath;
            this.requestMethod = requestMethod;
            this.timestamp = timestamp;
            this.suggestion = suggestion;
            this.exceptionType = exceptionType;
        }
    }
}