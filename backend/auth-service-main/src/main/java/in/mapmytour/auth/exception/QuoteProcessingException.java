package in.mapmytour.auth.exception;

public class QuoteProcessingException extends RuntimeException {
    public QuoteProcessingException(String message) {
        super(message);
    }

    public QuoteProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}