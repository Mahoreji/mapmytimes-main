package in.mapmytour.blog.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BlogException {
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }
}
