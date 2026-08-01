package in.mapmytour.auth.security.oauth2;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import in.mapmytour.auth.utils.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int cookieExpireSeconds = 600; // 10 minutes (increased from 3 minutes)

    @Value("${app.oauth2.cookie.secure:true}")
    private boolean cookieSecure;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.debug("Loading authorization request from cookie: {}", OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        OAuth2AuthorizationRequest authRequest = CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    try {
                        log.debug("Found authorization request cookie, deserializing...");
                        return CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize authorization request cookie", e);
                        return null;
                    }
                })
                .orElse(null);
        
        if (authRequest == null) {
            log.warn("No authorization request found in cookies. Available cookies: {}", 
                    request.getCookies() != null ? java.util.Arrays.stream(request.getCookies())
                            .map(c -> c.getName()).collect(java.util.stream.Collectors.joining(", ")) : "none");
        } else {
            log.debug("Successfully loaded authorization request for state: {}", authRequest.getState());
        }
        return authRequest;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            log.debug("Removing authorization request cookies");
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        log.debug("Saving authorization request to cookie. State: {}, RegistrationId: {}", 
                authorizationRequest.getState(), authorizationRequest.getAttribute("registration_id"));
        
        // Use secure cookies in production (HTTPS), non-secure in development
        CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtils.serialize(authorizationRequest), cookieExpireSeconds, true, cookieSecure);
        
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
            log.debug("Saving redirect URI to cookie: {}", redirectUriAfterLogin);
            CookieUtils.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, 
                    cookieExpireSeconds, true, cookieSecure);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}