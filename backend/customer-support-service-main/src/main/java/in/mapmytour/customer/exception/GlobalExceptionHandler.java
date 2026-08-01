package in.mapmytour.customer.exception;

import in.mapmytour.customer.dto.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>builder()
                .success(false)
                .statusCode(404)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<APIResponse<Void>> handleServiceException(
            ServiceException ex, WebRequest request) {
        log.error("Service error: {}", ex.getMessage(), ex);

        APIResponse<Void> response = APIResponse.<Void>builder()
                .success(false)
                .statusCode(500)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>builder()
                .success(false)
                .statusCode(403)
                .message("Access denied: " + ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.add(fieldName + ": " + errorMessage);
        });

        APIResponse<Void> response = APIResponse.<Void>builder()
                .success(false)
                .statusCode(400)
                .message("Validation failed")
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());

        String message = "Data integrity violation. Please check your request.";
        if (ex.getMessage().contains("duplicate key")) {
            message = "Duplicate entry. Record already exists.";
        }

        APIResponse<Void> response = APIResponse.<Void>builder()
                .success(false)
                .statusCode(409)
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Void>> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex, WebRequest request) {
        log.error("Invalid request body: {}", ex.getMessage());
        
        String message = "Invalid request body. Please check your JSON format.";
        if (ex.getMessage() != null && ex.getMessage().contains("JSON parse error")) {
            message = "Invalid JSON format in request body. Please ensure your request is valid JSON.";
        }

        APIResponse<Void> response = APIResponse.<Void>builder()
                .success(false)
                .statusCode(400)
                .message(message)
                .errors(List.of(ex.getMessage() != null ? ex.getMessage() : "Invalid request format"))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIResponse<Void>> handleMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.error("Method not supported: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.<Void>builder()
                .success(false)
                .statusCode(405)
                .message("Method not allowed: " + ex.getMethod())
                .errors(List.of(ex.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleGeneralException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        APIResponse<Void> response = APIResponse.<Void>builder()
                .success(false)
                .statusCode(500)
                .message("An unexpected error occurred. Please try again later.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}