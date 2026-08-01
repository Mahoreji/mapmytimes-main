package in.mapmytour.blog.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BlogException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}