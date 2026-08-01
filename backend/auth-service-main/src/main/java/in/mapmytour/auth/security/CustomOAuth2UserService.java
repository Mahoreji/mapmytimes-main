package in.mapmytour.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            log.info("OAuth2 user loaded successfully for provider: {}", registrationId);
            log.debug("OAuth2 user attributes: {}", oAuth2User.getAttributes());

            return oAuth2User;
        } catch (OAuth2AuthenticationException ex) {
            log.error("OAuth2 authentication failed: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during OAuth2 user loading: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException("user_info_error");
        }
    }
}