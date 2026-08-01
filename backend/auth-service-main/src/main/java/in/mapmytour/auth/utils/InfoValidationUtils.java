package in.mapmytour.auth.utils;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

@UtilityClass
public class InfoValidationUtils {

    /**
     * Validate FAQ question
     */
    public boolean isValidQuestion(String question) {
        return StringUtils.hasText(question) &&
                question.trim().length() >= 5 &&
                question.trim().length() <= 500;
    }

    /**
     * Validate FAQ answer
     */
    public boolean isValidAnswer(String answer) {
        return StringUtils.hasText(answer) &&
                answer.trim().length() >= 10 &&
                answer.trim().length() <= 2000;
    }

    /**
     * Validate FAQ category
     */
    public boolean isValidCategory(String category) {
        return category == null ||
                (category.trim().length() > 0 && category.trim().length() <= 100);
    }

    /**
     * Sanitize input string
     */
    public String sanitizeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        return StringUtils.hasText(email) &&
                email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Validate phone number format
     */
    public boolean isValidPhoneNumber(String phone) {
        return StringUtils.hasText(phone) &&
                phone.matches("^[+]?[0-9\\s\\-\\(\\)]{10,15}$");
    }
}