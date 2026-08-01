package in.mapmytour.blog.exception;

import in.mapmytour.blog.dto.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BlogException.class)
    public ResponseEntity<APIResponse<Object>> handleBlogException(BlogException ex, WebRequest request) {
        log.error("Blog exception occurred: {}", ex.getMessage(), ex);

        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(ex.getStatus().value())
                .message(ex.getMessage())
                .errors(Arrays.asList(ex.getCode()))
                .build();

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation error occurred: {}", ex.getMessage());

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("Type mismatch error: {}", ex.getMessage());

        String error = "Invalid value for parameter: " + ex.getName();

        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Invalid parameter type")
                .errors(Arrays.asList(error))
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("HTTP message not readable: {}", ex.getMessage());

        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Invalid request body")
                .errors(Arrays.asList("INVALID_JSON"))
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<APIResponse<Object>> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        log.error("Missing request part: {}", ex.getMessage());

        String error = "Missing required part: " + ex.getRequestPartName();

        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Missing required request part")
                .errors(Arrays.asList(error))
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<APIResponse<Object>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.error("File size exceeded: {}", ex.getMessage());

        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .message("File size exceeds maximum allowed size")
                .errors(Arrays.asList("FILE_SIZE_EXCEEDED"))
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<APIResponse<Object>> handleMultipartException(MultipartException ex) {
        Throwable current = ex;
        while (current != null) {
            String name = current.getClass().getName();
            if ("org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException".equals(name)) {
                APIResponse<Object> response = APIResponse.builder()
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Too many multipart parts in request")
                        .errors(List.of("MULTIPART_PART_COUNT_EXCEEDED"))
                        .build();
                return ResponseEntity.badRequest().body(response);
            }
            current = current.getCause();
        }

        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Invalid multipart request")
                .errors(List.of("INVALID_MULTIPART_REQUEST"))
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());

        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .errors(Arrays.asList("INVALID_ARGUMENT"))
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Object>> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        
        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(message)
                .errors(Arrays.asList("INTERNAL_SERVER_ERROR"))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<APIResponse<Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.error("Unsupported Media Type: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(APIResponse.builder()
                        .success(false)
                        .statusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                        .message("Unsupported content type. Please use 'multipart/form-data' with JSON in 'post' and files in 'mediaFiles'.")
                        .errors(List.of(ex.getMessage()))
                        .build());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<APIResponse<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .message("Not found")
                .errors(List.of("NOT_FOUND"))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
