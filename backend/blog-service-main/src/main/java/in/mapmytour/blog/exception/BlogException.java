package in.mapmytour.blog.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BlogException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public BlogException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = status.name();
    }

    public BlogException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public BlogException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
        this.code = status.name();
    }
}