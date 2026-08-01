package in.mapmytour.blog.exception;

import org.springframework.http.HttpStatus;

public class FileUploadException extends BlogException {
    public FileUploadException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "FILE_UPLOAD_ERROR");
    }
}
