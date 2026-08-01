package in.mapmytour.auth.exception;

public class QuoteAccessDeniedException extends RuntimeException {
    public QuoteAccessDeniedException(String message) {
        super(message);
    }
}