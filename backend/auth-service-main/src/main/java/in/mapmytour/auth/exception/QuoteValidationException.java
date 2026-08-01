package in.mapmytour.auth.exception;

import java.util.List;

public class QuoteValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public QuoteValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}