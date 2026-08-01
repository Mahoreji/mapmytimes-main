package in.mapmytour.blog.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BlogException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
