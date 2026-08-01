package in.mapmytour.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationPropertiesValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${aws.s3.access-key}")
    private String awsAccessKey;

    @Value("${spring.mail.username}")
    private String emailUsername;

    @Value("${twilio.account-sid}")
    private String twilioAccountSid;

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        log.info("Validating application configuration...");

        boolean isValid = true;

        if (jwtSecret == null || jwtSecret.trim().isEmpty() || jwtSecret.equals("mapMyTourSecretKey2024!")) {
            log.warn("JWT secret is not properly configured. Please set a strong secret key.");
            isValid = false;
        }

        if (awsAccessKey == null || awsAccessKey.equals("your-access-key")) {
            log.warn("AWS credentials are not properly configured.");
            isValid = false;
        }

        if (emailUsername == null || emailUsername.equals("your-email@gmail.com")) {
            log.warn("Email configuration is not properly configured.");
            isValid = false;
        }

        if (twilioAccountSid == null || twilioAccountSid.equals("your-account-sid")) {
            log.warn("Twilio configuration is not properly configured.");
            isValid = false;
        }

        if (isValid) {
            log.info("Application configuration validated successfully!");
        } else {
            log.warn("Some configurations are using default values. Please update them for production use.");
        }
    }
}