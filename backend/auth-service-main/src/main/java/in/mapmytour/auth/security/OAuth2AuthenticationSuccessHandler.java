// src/main/java/in/mapmytour/auth/security/OAuth2AuthenticationSuccessHandler.java
package in.mapmytour.auth.security;

import in.mapmytour.auth.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import in.mapmytour.auth.utils.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.frontend.url}")
    private String defaultFrontendUrl;

    @Value("${app.oauth2.allowed-redirect-uris:}")
    private String allowedRedirectUris;

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    public OAuth2AuthenticationSuccessHandler(HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository) {
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        long startTime = System.currentTimeMillis();
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info("OAuth2 success handler started for user: {} at {}", 
                Optional.ofNullable(oAuth2User.getAttribute("email")), startTime);

        // Extract user data
        String email = oAuth2User.getAttribute("email");
        String firstName = getFirstName(oAuth2User);
        String lastName = getLastName(oAuth2User);
        String picture = oAuth2User.getAttribute("picture");
        String provider = getProvider(request);
        String providerId = getProviderId(oAuth2User, provider);

        // Get redirect URI from cookie (set during OAuth2 login initiation)
        // This allows different frontends (www.mapmytour.in, staging.mapmytour.in) to use the same backend
        String redirectUri = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(cookie -> cookie.getValue())
                .orElse(null);

        // Determine frontend URL: use redirect_uri if provided, otherwise use default
        String frontendUrl = defaultFrontendUrl;
        if (redirectUri != null && !redirectUri.trim().isEmpty()) {
            // Validate redirect_uri against allowed list (security check)
            if (isRedirectUriAllowed(redirectUri)) {
                // Extract base URL from redirect_uri (e.g., https://www.mapmytour.in/auth/oauth2/redirect -> https://www.mapmytour.in)
                try {
                    java.net.URI uri = java.net.URI.create(redirectUri);
                    frontendUrl = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
                    log.info("Using redirect_uri from cookie: {} -> {}", redirectUri, frontendUrl);
                } catch (Exception e) {
                    log.warn("Failed to parse redirect_uri from cookie: {}, using default frontend URL", redirectUri, e);
                }
            } else {
                log.warn("Redirect URI not in allowed list: {}, using default frontend URL", redirectUri);
            }
        }
        
        log.debug("Determined frontend URL: {}", frontendUrl);

        log.debug("Building redirect URL for user: {}", email);
        // Build redirect URL with user data
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/oauth2/redirect")
                .queryParam("email", URLEncoder.encode(email != null ? email : "", StandardCharsets.UTF_8))
                .queryParam("firstName", URLEncoder.encode(firstName != null ? firstName : "", StandardCharsets.UTF_8))
                .queryParam("lastName", URLEncoder.encode(lastName != null ? lastName : "", StandardCharsets.UTF_8))
                .queryParam("picture", URLEncoder.encode(picture != null ? picture : "", StandardCharsets.UTF_8))
                .queryParam("provider", provider)
                .queryParam("providerId", URLEncoder.encode(providerId != null ? providerId : "", StandardCharsets.UTF_8))
                .queryParam("success", "true")
                .build().toUriString();

        log.info("Redirecting to: {}", redirectUrl);
        
        // Clean up OAuth2 cookies
        log.debug("Cleaning up OAuth2 cookies");
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("OAuth2 success handler completed in {} ms. Redirecting now.", duration);
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String getProvider(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (requestURI.contains("google")) {
            return "google";
        } else if (requestURI.contains("facebook")) {
            return "facebook";
        }
        return "unknown";
    }

    private String getFirstName(OAuth2User oAuth2User) {
        // For Google
        String givenName = oAuth2User.getAttribute("given_name");
        if (givenName != null) return givenName;

        // For Facebook
        String firstName = oAuth2User.getAttribute("first_name");
        if (firstName != null) return firstName;

        // Fallback: extract from name
        String name = oAuth2User.getAttribute("name");
        if (name != null && name.contains(" ")) {
            return name.split(" ")[0];
        }

        return name != null ? name : "";
    }

    private String getLastName(OAuth2User oAuth2User) {
        // For Google
        String familyName = oAuth2User.getAttribute("family_name");
        if (familyName != null) return familyName;

        // For Facebook
        String lastName = oAuth2User.getAttribute("last_name");
        if (lastName != null) return lastName;

        // Fallback: extract from name
        String name = oAuth2User.getAttribute("name");
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ");
            return parts.length > 1 ? parts[parts.length - 1] : "";
        }

        return "";
    }

    private String getProviderId(OAuth2User oAuth2User, String provider) {
        if ("google".equals(provider)) {
            return oAuth2User.getAttribute("sub");
        } else if ("facebook".equals(provider)) {
            return oAuth2User.getAttribute("id");
        }
        return oAuth2User.getAttribute("id");
    }

    /**
     * Validate that the redirect_uri is in the allowed list
     */
    private boolean isRedirectUriAllowed(String redirectUri) {
        if (allowedRedirectUris == null || allowedRedirectUris.trim().isEmpty()) {
            log.warn("No allowed redirect URIs configured, allowing all");
            return true; // If not configured, allow all (for backward compatibility)
        }

        String[] allowedUris = allowedRedirectUris.split(",");
        for (String allowedUri : allowedUris) {
            if (redirectUri.trim().equalsIgnoreCase(allowedUri.trim())) {
                return true;
            }
        }

        return false;
    }
}