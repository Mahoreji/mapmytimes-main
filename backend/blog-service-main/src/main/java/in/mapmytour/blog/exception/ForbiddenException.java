package in.mapmytour.blog.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BlogException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
