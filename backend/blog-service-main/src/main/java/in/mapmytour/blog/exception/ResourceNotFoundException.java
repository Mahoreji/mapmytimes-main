package in.mapmytour.blog.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BlogException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
