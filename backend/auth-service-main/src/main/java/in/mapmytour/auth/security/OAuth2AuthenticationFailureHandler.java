// src/main/java/in/mapmytour/auth/security/OAuth2AuthenticationFailureHandler.java
package in.mapmytour.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        // Enhanced logging for OAuth2 errors
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = requestUri + (queryString != null ? "?" + queryString : "");
        
        log.error("OAuth2 authentication failed - Request URI: {}", fullUrl);
        log.error("OAuth2 authentication failed - Exception type: {}", exception.getClass().getName());
        log.error("OAuth2 authentication failed - Exception message: {}", exception.getMessage());
        
        // Log OAuth2-specific error details if available
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            OAuth2Error error = oauth2Exception.getError();
            log.error("OAuth2 Error Code: {}", error.getErrorCode());
            log.error("OAuth2 Error Description: {}", error.getDescription());
            log.error("OAuth2 Error URI: {}", error.getUri());
            
            // Log additional context for debugging
            log.error("Request Method: {}", request.getMethod());
            log.error("Request Headers: {}", java.util.Collections.list(request.getHeaderNames()));
            
            // Check for redirect_uri in query string
            String queryStr = request.getQueryString();
            if (queryStr != null) {
                log.error("Query String: {}", queryStr);
                if (queryStr.contains("redirect_uri")) {
                    String redirectUri = java.util.Arrays.stream(queryStr.split("&"))
                        .filter(param -> param.startsWith("redirect_uri="))
                        .findFirst()
                        .map(param -> {
                            try {
                                return java.net.URLDecoder.decode(param.substring("redirect_uri=".length()), java.nio.charset.StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                return param.substring("redirect_uri=".length());
                            }
                        })
                        .orElse("NOT_FOUND");
                    log.error("Redirect URI from query: {}", redirectUri);
                }
            }
        }
        
        // Log exception stack trace for debugging
        log.error("OAuth2 authentication failure stack trace:", exception);

        // Build error message for frontend
        String errorMessage = exception.getMessage();
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            OAuth2Error error = oauth2Exception.getError();
            errorMessage = String.format("OAuth2 Error: [%s] %s", 
                error.getErrorCode(), 
                error.getDescription() != null ? error.getDescription() : errorMessage);
        }

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/oauth2/redirect")
                .queryParam("error", errorMessage)
                .queryParam("error_code", exception instanceof OAuth2AuthenticationException ? 
                    ((OAuth2AuthenticationException) exception).getError().getErrorCode() : "UNKNOWN")
                .queryParam("success", "false")
                .build().toUriString();

        log.info("Redirecting to frontend with error: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}