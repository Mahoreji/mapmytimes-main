package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.auth.AuthResponse;
import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.auth.OAuth2LinkRequest;
import in.mapmytour.auth.dto.auth.OAuth2LoginRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for OAuth2 authentication operations
 *
 * This service handles OAuth2 integration for Google and Facebook providers,
 * supporting both first-time user creation and existing user authentication.
 */
public interface OAuthService {

    /**
     * Process OAuth2 login for new and existing users
     *
     * For first-time users:
     * - Creates new user account
     * - Sets email as password (encoded)
     * - Extracts user details from OAuth provider
     * - Sends welcome email
     *
     * For existing users:
     * - Updates profile information from OAuth provider
     * - Handles provider conflicts
     * - Maintains existing preferences
     *
     * @param request OAuth2 login request containing user data from provider
     * @param httpServletRequest HTTP request for extracting IP and user agent
     * @return AuthResponse containing authentication tokens and user data
     * @throws RuntimeException if authentication fails
     * @throws IllegalArgumentException if request validation fails
     */
    AuthResponse processOAuth2Login(OAuth2LoginRequest request, HttpServletRequest httpServletRequest);

    /**
     * Link an OAuth2 account to an existing authenticated user
     *
     * @param userEmail Email of the authenticated user
     * @param request OAuth2 link request containing provider data
     * @return MessageResponse indicating success or failure
     * @throws RuntimeException if linking fails
     * @throws IllegalArgumentException if validation fails or account already linked
     */
    MessageResponse linkOAuth2Account(String userEmail, OAuth2LinkRequest request);

    /**
     * Unlink an OAuth2 account from a user
     *
     * @param userEmail Email of the authenticated user
     * @param provider OAuth2 provider to unlink (google, facebook)
     * @return MessageResponse indicating success or failure
     * @throws RuntimeException if unlinking fails
     * @throws IllegalArgumentException if validation fails or account not linked
     */
    MessageResponse unlinkOAuth2Account(String userEmail, String provider);
}