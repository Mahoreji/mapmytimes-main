package in.mapmytour.auth.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Slf4j
public class ValidationUtil {

    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+\\d{1,3}[- ]?)?\\d{10}$"
    );

    private static final Pattern INDIAN_PHONE_PATTERN = Pattern.compile(
            "^(\\+91[\\-\\s]?)?[0]?(91)?[789]\\d{9}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
    );

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9]+$"
    );

    private static final Pattern ALPHABETIC_PATTERN = Pattern.compile(
            "^[a-zA-Z\\s]+$"
    );

    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "^\\d+$"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    );

    // String validation methods
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }

    public static boolean hasText(String str) {
        return StringUtils.hasText(str);
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    // Email validation
    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // Phone number validation
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (isNullOrEmpty(phoneNumber)) {
            return false;
        }

        String cleanedNumber = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        return PHONE_PATTERN.matcher(cleanedNumber).matches();
    }

    public static boolean isValidIndianPhoneNumber(String phoneNumber) {
        if (isNullOrEmpty(phoneNumber)) {
            return false;
        }

        String cleanedNumber = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        return INDIAN_PHONE_PATTERN.matcher(cleanedNumber).matches();
    }

    // Password validation
    public static boolean isValidPassword(String password) {
        if (isNullOrEmpty(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isStrongPassword(String password) {
        if (isNullOrEmpty(password)) {
            return false;
        }

        return password.length() >= 8 &&
                password.matches(".*[0-9].*") &&          // Contains digit
                password.matches(".*[a-z].*") &&          // Contains lowercase
                password.matches(".*[A-Z].*") &&          // Contains uppercase
                password.matches(".*[@#$%^&+=].*") &&     // Contains special char
                !password.contains(" ");                   // No spaces
    }

    public static String getPasswordStrengthMessage(String password) {
        if (isNullOrEmpty(password)) {
            return "Password cannot be empty";
        }

        StringBuilder message = new StringBuilder();

        if (password.length() < 8) {
            message.append("Password must be at least 8 characters long. ");
        }

        if (!password.matches(".*[0-9].*")) {
            message.append("Password must contain at least one digit. ");
        }

        if (!password.matches(".*[a-z].*")) {
            message.append("Password must contain at least one lowercase letter. ");
        }

        if (!password.matches(".*[A-Z].*")) {
            message.append("Password must contain at least one uppercase letter. ");
        }

        if (!password.matches(".*[@#$%^&+=].*")) {
            message.append("Password must contain at least one special character (@#$%^&+=). ");
        }

        if (password.contains(" ")) {
            message.append("Password cannot contain spaces. ");
        }

        return message.length() > 0 ? message.toString().trim() : "Password is strong";
    }

    // Pattern-based validations
    public static boolean isAlphanumeric(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(str).matches();
    }

    public static boolean isAlphabetic(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return ALPHABETIC_PATTERN.matcher(str).matches();
    }

    public static boolean isNumeric(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return NUMERIC_PATTERN.matcher(str).matches();
    }

    public static boolean isValidUrl(String url) {
        if (isNullOrEmpty(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }

    // Length validations
    public static boolean hasMinLength(String str, int minLength) {
        return isNotNullOrEmpty(str) && str.length() >= minLength;
    }

    public static boolean hasMaxLength(String str, int maxLength) {
        return str == null || str.length() <= maxLength;
    }

    public static boolean hasLengthBetween(String str, int minLength, int maxLength) {
        return hasMinLength(str, minLength) && hasMaxLength(str, maxLength);
    }

    // Numeric validations
    public static boolean isPositiveNumber(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }

        try {
            double number = Double.parseDouble(str);
            return number > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNonNegativeNumber(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }

        try {
            double number = Double.parseDouble(str);
            return number >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidInteger(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidDouble(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Range validations
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    // Name validations
    public static boolean isValidName(String name) {
        if (isNullOrEmpty(name)) {
            return false;
        }

        return name.trim().length() >= 2 &&
                name.trim().length() <= 50 &&
                name.matches("^[a-zA-Z\\s.'-]+$");
    }

    public static boolean isValidUsername(String username) {
        if (isNullOrEmpty(username)) {
            return false;
        }

        return username.length() >= 3 &&
                username.length() <= 30 &&
                username.matches("^[a-zA-Z0-9._-]+$") &&
                !username.startsWith(".") &&
                !username.endsWith(".") &&
                !username.contains("..");
    }

    // PIN/OTP validations
    public static boolean isValidPin(String pin) {
        return isNotNullOrEmpty(pin) &&
                pin.matches("^\\d{4,6}$");
    }

    public static boolean isValidOtp(String otp) {
        return isNotNullOrEmpty(otp) &&
                otp.matches("^\\d{4,8}$");
    }

    // Postal code validations
    public static boolean isValidIndianPincode(String pincode) {
        if (isNullOrEmpty(pincode)) {
            return false;
        }
        return pincode.matches("^[1-9][0-9]{5}$");
    }

    public static boolean isValidUSZipCode(String zipCode) {
        if (isNullOrEmpty(zipCode)) {
            return false;
        }
        return zipCode.matches("^\\d{5}(-\\d{4})?$");
    }

    // Credit card validation (Luhn algorithm)
    public static boolean isValidCreditCard(String cardNumber) {
        if (isNullOrEmpty(cardNumber)) {
            return false;
        }

        String cleanedNumber = cardNumber.replaceAll("[\\s-]", "");

        if (!cleanedNumber.matches("^\\d{13,19}$")) {
            return false;
        }

        return luhnCheck(cleanedNumber);
    }

    private static boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10) == 0;
    }

    // Age validation
    public static boolean isValidAge(int age) {
        return age >= 0 && age <= 150;
    }

    public static boolean isAdult(int age) {
        return age >= 18;
    }

    public static boolean isMinor(int age) {
        return age < 18;
    }

    // Common business validations
    public static boolean isValidGstNumber(String gstNumber) {
        if (isNullOrEmpty(gstNumber)) {
            return false;
        }

        return gstNumber.matches("^\\d{2}[A-Z]{5}\\d{4}[A-Z][A-Z\\d][Z][A-Z\\d]$");
    }

    public static boolean isValidPanNumber(String panNumber) {
        if (isNullOrEmpty(panNumber)) {
            return false;
        }

        return panNumber.matches("^[A-Z]{5}\\d{4}[A-Z]$");
    }

    public static boolean isValidAadharNumber(String aadharNumber) {
        if (isNullOrEmpty(aadharNumber)) {
            return false;
        }

        String cleanedAadhar = aadharNumber.replaceAll("[\\s-]", "");
        return cleanedAadhar.matches("^\\d{12}$");
    }

    // Sanitization methods
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        return input.trim()
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
    }

    public static String removeSpecialCharacters(String input) {
        if (input == null) {
            return null;
        }

        return input.replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    public static String keepAlphanumeric(String input) {
        if (input == null) {
            return null;
        }

        return input.replaceAll("[^a-zA-Z0-9]", "");
    }
}