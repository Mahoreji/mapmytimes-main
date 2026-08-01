package in.mapmytour.blog.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BlogException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
