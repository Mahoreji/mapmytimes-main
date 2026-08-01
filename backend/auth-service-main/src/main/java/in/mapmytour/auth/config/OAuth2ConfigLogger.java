package in.mapmytour.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs OAuth2 configuration at startup for debugging purposes
 */
@Component
@Slf4j
public class OAuth2ConfigLogger {

    @Value("${spring.security.oauth2.client.registration.google.client-id:NOT_SET}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:NOT_SET}")
    private String googleRedirectUri;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id:NOT_SET}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.redirect-uri:NOT_SET}")
    private String facebookRedirectUri;

    @Value("${app.oauth2.base-url:NOT_SET}")
    private String oauth2BaseUrl;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:NOT_SET}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret:NOT_SET}")
    private String facebookClientSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void logOAuth2Configuration() {
        log.info("=== OAuth2 Configuration ===");
        log.info("OAuth2 Base URL: {}", oauth2BaseUrl);
        log.info("Google Client ID: {}", maskSensitive(googleClientId));
        log.info("Google Client Secret: {}", maskSensitive(googleClientSecret));
        log.info("Google Redirect URI (raw): {}", googleRedirectUri);
        log.info("Facebook Client ID: {}", facebookClientId);
        log.info("Facebook Client Secret: {}", maskSensitive(facebookClientSecret));
        log.info("Facebook Redirect URI (raw): {}", facebookRedirectUri);
        log.info("===========================");
        
        // Check for placeholder/default secrets
        if (googleClientSecret == null || googleClientSecret.contains("012345") || googleClientSecret.contains("NOT_SET") || googleClientSecret.length() < 20) {
            log.error("✗✗✗ CRITICAL ERROR: Google Client Secret appears to be a placeholder or invalid!");
            log.error("   Current value: {} (masked)", maskSensitive(googleClientSecret));
            log.error("   This will cause OAuth2 token exchange to fail with 401!");
            log.error("   Set GOOGLE_CLIENT_SECRET environment variable with the actual secret from Google Cloud Console");
        }
        if (facebookClientSecret == null || facebookClientSecret.contains("NOT_SET") || facebookClientSecret.length() < 10) {
            log.error("✗✗✗ CRITICAL ERROR: Facebook Client Secret appears to be invalid!");
            log.error("   Current value: {} (masked)", maskSensitive(facebookClientSecret));
            log.error("   This will cause OAuth2 token exchange to fail with 401!");
            log.error("   Set FACEBOOK_CLIENT_SECRET environment variable with the actual secret from Facebook Developers");
        }
        
        // Validate redirect URIs and show what will be used
        if (googleRedirectUri.contains("{registrationId}")) {
            String googleCallback = googleRedirectUri.replace("{registrationId}", "google");
            log.info("✓ Google Callback URL will be: {}", googleCallback);
            log.warn("⚠️  MAKE SURE this URL is registered in Google Cloud Console: {}", googleCallback);
        } else {
            log.error("✗ ERROR: Google Redirect URI missing {registrationId} placeholder: {}", googleRedirectUri);
        }
        
        if (facebookRedirectUri.contains("{registrationId}")) {
            String facebookCallback = facebookRedirectUri.replace("{registrationId}", "facebook");
            log.info("✓ Facebook Callback URL will be: {}", facebookCallback);
            log.warn("⚠️  MAKE SURE this URL is registered in Facebook Developers: {}", facebookCallback);
        } else {
            log.error("✗ ERROR: Facebook Redirect URI missing {registrationId} placeholder: {}", facebookRedirectUri);
        }
        
        // Check for common issues
        if (googleRedirectUri.contains("staging") && !oauth2BaseUrl.contains("staging")) {
            log.warn("⚠️  WARNING: Google redirect URI uses 'staging' but base URL doesn't match!");
        }
        if (facebookRedirectUri.contains("staging") && !oauth2BaseUrl.contains("staging")) {
            log.warn("⚠️  WARNING: Facebook redirect URI uses 'staging' but base URL doesn't match!");
        }
        
        // Critical validation
        if (!googleRedirectUri.contains("{registrationId}")) {
            log.error("✗✗✗ CRITICAL ERROR: Google redirect URI missing {registrationId} placeholder!");
            log.error("   Current value: {}", googleRedirectUri);
            log.error("   This will cause OAuth2 token exchange to fail!");
        }
        if (!facebookRedirectUri.contains("{registrationId}")) {
            log.error("✗✗✗ CRITICAL ERROR: Facebook redirect URI missing {registrationId} placeholder!");
            log.error("   Current value: {}", facebookRedirectUri);
            log.error("   This will cause OAuth2 token exchange to fail!");
        }
    }

    private String maskSensitive(String value) {
        if (value == null || value.equals("NOT_SET") || value.length() < 10) {
            return value != null ? value : "null";
        }
        try {
            return value.substring(0, 10) + "..." + value.substring(value.length() - 4);
        } catch (Exception e) {
            return "***masked***";
        }
    }
}

