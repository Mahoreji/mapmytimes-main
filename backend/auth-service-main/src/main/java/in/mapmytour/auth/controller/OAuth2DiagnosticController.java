package in.mapmytour.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic endpoint to check OAuth2 configuration
 * Useful for troubleshooting OAuth2 401 errors
 */
@RestController
@RequestMapping("/api/v1/auth/oauth2/diagnostic")
@RequiredArgsConstructor
@Slf4j
public class OAuth2DiagnosticController {

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

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getOAuth2Configuration() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // Google Configuration
            Map<String, Object> google = new HashMap<>();
            google.put("clientId", maskSensitive(googleClientId != null ? googleClientId : "NOT_SET"));
            google.put("clientSecret", maskSensitive(googleClientSecret != null ? googleClientSecret : "NOT_SET"));
            google.put("redirectUri", googleRedirectUri != null ? googleRedirectUri : "NOT_SET");
            
            String googleCallbackUrl = "ERROR: Invalid redirect URI";
            if (googleRedirectUri != null && googleRedirectUri.contains("{registrationId}")) {
                googleCallbackUrl = googleRedirectUri.replace("{registrationId}", "google");
            } else if (googleRedirectUri != null) {
                googleCallbackUrl = "ERROR: Missing {registrationId} placeholder";
            }
            google.put("actualCallbackUrl", googleCallbackUrl);
            
            // Check if client secret is placeholder
            boolean googleSecretValid = googleClientSecret != null && 
                                       !googleClientSecret.contains("012345") && 
                                       !googleClientSecret.equals("NOT_SET") && 
                                       googleClientSecret.length() >= 20;
            google.put("clientSecretValid", googleSecretValid);
            if (!googleSecretValid) {
                google.put("clientSecretWarning", "Client secret appears to be a placeholder. Set GOOGLE_CLIENT_SECRET environment variable.");
            }
            config.put("google", google);
            
            // Facebook Configuration
            Map<String, Object> facebook = new HashMap<>();
            facebook.put("clientId", facebookClientId != null ? facebookClientId : "NOT_SET");
            facebook.put("clientSecret", maskSensitive(facebookClientSecret != null ? facebookClientSecret : "NOT_SET"));
            facebook.put("redirectUri", facebookRedirectUri != null ? facebookRedirectUri : "NOT_SET");
            
            String facebookCallbackUrl = "ERROR: Invalid redirect URI";
            if (facebookRedirectUri != null && facebookRedirectUri.contains("{registrationId}")) {
                facebookCallbackUrl = facebookRedirectUri.replace("{registrationId}", "facebook");
            } else if (facebookRedirectUri != null) {
                facebookCallbackUrl = "ERROR: Missing {registrationId} placeholder";
            }
            facebook.put("actualCallbackUrl", facebookCallbackUrl);
            
            // Check if client secret is valid
            boolean facebookSecretValid = facebookClientSecret != null && 
                                         !facebookClientSecret.equals("NOT_SET") && 
                                         facebookClientSecret.length() >= 10;
            facebook.put("clientSecretValid", facebookSecretValid);
            if (!facebookSecretValid) {
                facebook.put("clientSecretWarning", "Client secret appears to be invalid. Set FACEBOOK_CLIENT_SECRET environment variable.");
            }
            config.put("facebook", facebook);
            
            // General Configuration
            config.put("oauth2BaseUrl", oauth2BaseUrl != null ? oauth2BaseUrl : "NOT_SET");
            config.put("message", "Check that the 'actualCallbackUrl' values are registered in OAuth2 providers");
            config.put("criticalIssue", !googleSecretValid || !facebookSecretValid);
            if (!googleSecretValid || !facebookSecretValid) {
                config.put("errorMessage", "One or more client secrets are invalid. This will cause OAuth2 token exchange to fail with 401.");
            }
            config.put("success", true);
            
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error in diagnostic endpoint", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("statusCode", 500);
            error.put("message", e.getMessage() != null ? e.getMessage() : "Internal server error");
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
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

