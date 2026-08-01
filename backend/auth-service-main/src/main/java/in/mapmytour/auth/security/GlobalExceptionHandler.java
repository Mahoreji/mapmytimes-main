package in.mapmytour.auth.security;

import in.mapmytour.auth.dto.APIResponse;
import in.mapmytour.auth.exception.ProfileNotPublicException;
import in.mapmytour.auth.utils.APIResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Helper method to explicitly set content type to JSON
     */
    private <T> ResponseEntity<APIResponse<T>> toJson(ResponseEntity<APIResponse<T>> entity) {
        return ResponseEntity
                .status(entity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(entity.getBody());
    }

    /**
     * Handle validation errors for request body
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.add(fieldName + ": " + errorMessage);
        });

        return toJson(APIResponseUtil.validationError("Validation failed", errors));
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<APIResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        log.warn("Constraint violation: {}", ex.getMessage());

        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        return toJson(APIResponseUtil.validationError("Validation failed", errors));
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<APIResponse<Object>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.warn("Authentication error: {}", ex.getMessage());

        String message = "Authentication failed";
        if (ex instanceof BadCredentialsException) {
            message = "Invalid email or password";
        } else if (ex instanceof DisabledException) {
            message = "Account is disabled";
        } else if (ex instanceof LockedException) {
            message = "Account is locked";
        }

        return toJson(APIResponseUtil.unauthorized(message));
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Access denied: {}", ex.getMessage());
        return toJson(APIResponseUtil.forbidden("Access denied. You don't have permission to access this resource."));
    }

    /**
     * Handle profile not public exceptions
     */
    @ExceptionHandler(ProfileNotPublicException.class)
    public ResponseEntity<APIResponse<Object>> handleProfileNotPublicException(
            ProfileNotPublicException ex, WebRequest request) {

        log.warn("Profile not public: {}", ex.getMessage());
        return toJson(APIResponseUtil.forbidden(ex.getMessage()));
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());
        return toJson(APIResponseUtil.badRequest(ex.getMessage()));
    }

    /**
     * Handle runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<APIResponse<Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("Runtime exception: {}", ex.getMessage(), ex);

        // Don't expose internal errors to the client
        String message = "An error occurred while processing your request";

        // For specific runtime exceptions, provide more specific messages
        if (ex.getMessage() != null &&
                (ex.getMessage().contains("not found") ||
                        ex.getMessage().contains("does not exist"))) {
            return toJson(APIResponseUtil.notFound(ex.getMessage()));
        }

        return toJson(APIResponseUtil.internalServerError(message));
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIResponse<Object>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, WebRequest request) {

        log.warn("Missing request parameter: {}", ex.getMessage());

        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        return toJson(APIResponseUtil.badRequest(message));
    }

    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse<Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn("Method argument type mismatch: {}", ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());
        return toJson(APIResponseUtil.badRequest(message));
    }

    /**
     * Handle HTTP message not readable
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.warn("HTTP message not readable: {}", ex.getMessage());
        return toJson(APIResponseUtil.badRequest("Invalid request body format"));
    }

    /**
     * Handle HTTP request method not supported
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIResponse<Object>> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        log.warn("HTTP method not supported: {}", ex.getMessage());

        String message = String.format("Request method '%s' not supported", ex.getMethod());
        return toJson(APIResponseUtil.custom(HttpStatus.METHOD_NOT_ALLOWED, message, null));
    }

    /**
     * Handle HTTP media type not supported
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<APIResponse<Object>> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, WebRequest request) {

        log.warn("HTTP media type not supported: {}", ex.getMessage());

        String message = String.format("Media type '%s' not supported", ex.getContentType());
        return toJson(APIResponseUtil.custom(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, null));
    }

    /**
     * Handle no handler found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<APIResponse<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {

        log.warn("No handler found: {}", ex.getMessage());

        String message = String.format("Endpoint '%s %s' not found",
                ex.getHttpMethod(), ex.getRequestURL());
        return toJson(APIResponseUtil.notFound(message));
    }

    /**
     * Handle file upload exceptions
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<APIResponse<Object>> handleMaxSizeException(
            MaxUploadSizeExceededException ex, WebRequest request) {

        log.warn("File size exceeded: {}", ex.getMessage());
        return toJson(APIResponseUtil.badRequest("File size exceeds maximum allowed limit"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<APIResponse<Object>> handleMultipartException(
            MultipartException ex, WebRequest request) {

        log.warn("Multipart exception: {}", ex.getMessage());
        return toJson(APIResponseUtil.badRequest("Error processing file upload"));
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<APIResponse<Object>> handleDataAccessException(
            org.springframework.dao.DataAccessException ex, HttpServletRequest httpRequest) {
        log.error("Database error occurred: {}", ex.getMessage(), ex);
        log.error("Request URL: {} {}", httpRequest.getMethod(), httpRequest.getRequestURL());

        return toJson(APIResponseUtil.internalServerError("Database error occurred. Please try again later."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Object>> handleGlobalException(
            Exception ex, WebRequest request, HttpServletRequest httpRequest) {

        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        log.error("Request URL: {} {}", httpRequest.getMethod(), httpRequest.getRequestURL());
        log.error("Request headers: {}", getRequestHeaders(httpRequest));
        log.error("Exception class: {}", ex.getClass().getName());
        if (ex.getCause() != null) {
            log.error("Caused by: {}", ex.getCause().getMessage());
        }

        return toJson(APIResponseUtil.internalServerError("An unexpected error occurred. Please try again later."));
    }

    /**
     * Helper method to get request headers for logging
     */
    private String getRequestHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            headers.append(headerName).append(": ").append(request.getHeader(headerName)).append(", ");
        });
        return headers.toString();
    }
}