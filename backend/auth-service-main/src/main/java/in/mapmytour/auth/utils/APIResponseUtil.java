package in.mapmytour.auth.utils;

import in.mapmytour.auth.dto.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

public class APIResponseUtil {

    // Success responses
    public static <T> ResponseEntity<APIResponse<T>> success(T data) {
        return success(data, "Operation completed successfully");
    }

    public static <T> ResponseEntity<APIResponse<T>> success(T data, String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .errors(null)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> created(T data) {
        return created(data, "Resource created successfully");
    }

    public static <T> ResponseEntity<APIResponse<T>> created(T data, String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.CREATED.value())
                .message(message)
                .data(data)
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> noContent() {
        return noContent("Operation completed successfully");
    }

    public static <T> ResponseEntity<APIResponse<T>> error(String data) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(data)
                .data(null)
                .errors(null)
                .build();

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> errors(T data, String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .data(data)
                .errors(null)
                .build();

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> noContent(String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.NO_CONTENT.value())
                .message(message)
                .data(null)
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Error responses
    public static <T> ResponseEntity<APIResponse<T>> badRequest(String message) {
        return badRequest(message, (List<String>) null);
    }

    public static <T> ResponseEntity<APIResponse<T>> badRequest(String message, List<String> errors) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .data(null)
                .errors(errors)
                .build();

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> badRequest(String message, String... errors) {
        return badRequest(message, Arrays.asList(errors));
    }

    public static <T> ResponseEntity<APIResponse<T>> unauthorized() {
        return unauthorized("Unauthorized access");
    }

    public static <T> ResponseEntity<APIResponse<T>> unauthorized(String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .message(message)
                .data(null)
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> forbidden() {
        return forbidden("Access forbidden");
    }

    public static <T> ResponseEntity<APIResponse<T>> forbidden(String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.FORBIDDEN.value())
                .message(message)
                .data(null)
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> notFound() {
        return notFound("Resource not found");
    }

    public static <T> ResponseEntity<APIResponse<T>> notFound(String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .message(message)
                .data(null)
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> conflict() {
        return conflict("Resource conflict");
    }

    public static <T> ResponseEntity<APIResponse<T>> conflict(String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.CONFLICT.value())
                .message(message)
                .data(null)
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> unprocessableEntity(String message) {
        return unprocessableEntity(message, null);
    }

    public static <T> ResponseEntity<APIResponse<T>> unprocessableEntity(String message, List<String> errors) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .message(message)
                .data(null)
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> internalServerError() {
        return internalServerError("Internal server error occurred");
    }

    public static <T> ResponseEntity<APIResponse<T>> internalServerError(String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(message)
                .data(null)
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Custom status responses
    public static <T> ResponseEntity<APIResponse<T>> custom(HttpStatus status, String message, T data) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(status.is2xxSuccessful())
                .statusCode(status.value())
                .message(message)
                .data(data)
                .errors(null)
                .build();

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public static <T> ResponseEntity<APIResponse<T>> custom(HttpStatus status, String message, T data, List<String> errors) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(status.is2xxSuccessful())
                .statusCode(status.value())
                .message(message)
                .data(data)
                .errors(errors)
                .build();

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Validation responses
    public static <T> ResponseEntity<APIResponse<T>> validationError(List<String> errors) {
        return validationError("Validation failed", errors);
    }

    public static <T> ResponseEntity<APIResponse<T>> validationError(String message, List<String> errors) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .data(null)
                .errors(errors)
                .build();

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Pagination responses
    public static <T> ResponseEntity<APIResponse<T>> paginatedSuccess(T data, String message) {
        APIResponse<T> response = APIResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .errors(null)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Helper methods to create APIResponse objects without ResponseEntity
    public static <T> APIResponse<T> createSuccessResponse(T data, String message) {
        return APIResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .errors(null)
                .build();
    }

    public static <T> APIResponse<T> createErrorResponse(String message, List<String> errors) {
        return APIResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .data(null)
                .errors(errors)
                .build();
    }

    public static <T> APIResponse<T> createErrorResponse(HttpStatus status, String message) {
        return APIResponse.<T>builder()
                .success(false)
                .statusCode(status.value())
                .message(message)
                .data(null)
                .errors(null)
                .build();
    }

    // Method to check if response is successful
    public static <T> boolean isSuccessful(APIResponse<T> response) {
        return response != null && response.isSuccess() && response.getStatusCode() >= 200 && response.getStatusCode() < 300;
    }

    // Method to extract data safely
    public static <T> T extractData(APIResponse<T> response) {
        return isSuccessful(response) ? response.getData() : null;
    }

    // Method to extract error message
    public static <T> String extractErrorMessage(APIResponse<T> response) {
        if (response == null) {
            return "Unknown error occurred";
        }

        if (!response.isSuccess()) {
            return response.getMessage();
        }

        return null;
    }

    // Method to extract all errors
    public static <T> List<String> extractErrors(APIResponse<T> response) {
        if (response == null || response.isSuccess()) {
            return null;
        }

        return response.getErrors();
    }
}