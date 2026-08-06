package in.mapmytour.auth.controller;

import in.mapmytour.auth.dto.APIResponse;
import in.mapmytour.auth.dto.auth.AuthResponse;
import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.auth.OAuth2LinkRequest;
import in.mapmytour.auth.dto.auth.OAuth2LoginRequest;
import in.mapmytour.auth.service.OAuthService;
import in.mapmytour.auth.utils.APIResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final OAuthService oAuthService;

    // Add a concurrent map to track processing requests
    private final ConcurrentHashMap<String, Boolean> processingRequests = new ConcurrentHashMap<>();

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Base URL used to build the Spring Security OAuth2 authorization endpoint.
     *
     * - Local dev (default here): http://localhost:8081 (auth-service v2 direct port)
     * - MapMyTimes production: override APP_OAUTH2_BASE_URL env var to https://api.mapmytimes.com
     * - MapMyTour production: override APP_OAUTH2_BASE_URL env var to https://api.mapmytimes.com
     */
    @Value("${app.oauth2.base-url:http://localhost:8081}")
    private String oauth2BaseUrl;

    private String sanitizeBaseUrl(String raw) {
        if (raw == null) {
            return "http://localhost:8081";
        }
        String s = raw.trim();
        s = s.replace("`", "");
        s = s.replace("\"", "");
        s = s.replace("'", "");
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Initiate OAuth2 login.
     *
     * Default behaviour (browser click via <a href>): HTTP 302 redirect to the
     * provider's OAuth2 consent page, so users never see a raw JSON blob.
     *
     * For programmatic API consumers that want the URL string inside a JSON
     * response: pass the query parameter ?format=json (or ?format=json-pretty).
     *
     * Also supports ?redirect_uri= and ?redirectUri= as before.
     */
    @GetMapping("/login/{provider}")
    public Object initiateOAuth2Login(
            @PathVariable String provider,
            @RequestParam(name = "redirect_uri", required = false) String redirectUriParam,
            @RequestParam(name = "redirectUri", required = false) String redirectUriAlias,
            @RequestParam(name = "format", required = false) String format,
            HttpServletRequest request) {

        String redirectUri = (redirectUriParam != null && !redirectUriParam.isBlank()) ? redirectUriParam : redirectUriAlias;
        final String cleanBase = sanitizeBaseUrl(this.oauth2BaseUrl);
        final boolean wantJson = "json".equalsIgnoreCase(format) || "json-pretty".equalsIgnoreCase(format);

        try {
            log.info("Initiating OAuth2 login for provider: {} (jsonMode={}, redirectUri={})", provider, wantJson, redirectUri);

            // Validate provider
            if (!provider.equals("google") && !provider.equals("facebook")) {
                log.warn("Unsupported OAuth2 provider attempted: {}", provider);
                if (wantJson) {
                    return APIResponseUtil.badRequest("Unsupported OAuth2 provider: " + provider);
                }
                return "redirect:" + frontendUrl + "/login?error=unsupported_provider";
            }

            // Always build the authorization URL against the dedicated auth domain,
            // not against the gateway host, so that the OAuth2 handshake is handled
            // directly by the auth-service Spring Security configuration.
            String authorizationUrl = String.format("%s/oauth2/authorization/%s", cleanBase, provider);

            if (redirectUri != null && !redirectUri.trim().isEmpty()) {
                String encodedRedirectUri = java.net.URLEncoder.encode(redirectUri.trim(), java.nio.charset.StandardCharsets.UTF_8);
                authorizationUrl += (authorizationUrl.contains("?") ? "&" : "?") + "redirect_uri=" + encodedRedirectUri;
                log.info("OAuth2 redirect_uri parameter added: {}", redirectUri);
            }

            log.info("OAuth2 authorization URL generated for {}: {}", provider, authorizationUrl);

            if (wantJson) {
                return APIResponseUtil.success(authorizationUrl, "OAuth2 authorization URL generated");
            }
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(java.net.URI.create(authorizationUrl))
                    .build();

        } catch (Exception e) {
            log.error("Failed to initiate OAuth2 login for provider {}: {}", provider, e.getMessage(), e);
            if (wantJson) {
                return APIResponseUtil.error("Failed to initiate OAuth2 login");
            }
            return "redirect:" + frontendUrl + "/login?error=oauth_init_failed";
        }
    }

    /**
     * Handle OAuth2 callback with idempotency - ENHANCED WITH BETTER ERROR HANDLING
     */
    @PostMapping("/callback")
    public ResponseEntity<APIResponse<AuthResponse>> handleOAuth2Callback(
            @Valid @RequestBody OAuth2LoginRequest request,
            HttpServletRequest httpRequest) {

        String requestKey = request.getEmail() + ":" + request.getProvider();

        // Check if already processing this request
        if (processingRequests.putIfAbsent(requestKey, true) != null) {
            log.warn("OAuth2 callback already processing for: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(APIResponse.<AuthResponse>builder()
                            .success(false)
                            .message("Request already in progress, please try again")
                            .build());
        }

        try {
            log.info("Processing OAuth2 callback for: {} via {}", request.getEmail(), request.getProvider());

            // Enhanced validation
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return APIResponseUtil.badRequest("Email is required");
            }
            if (request.getProvider() == null || request.getProvider().trim().isEmpty()) {
                return APIResponseUtil.badRequest("Provider is required");
            }
            if (request.getProviderId() == null || request.getProviderId().trim().isEmpty()) {
                return APIResponseUtil.badRequest("Provider ID is required");
            }

            // Validate provider
            if (!request.getProvider().equals("google") && !request.getProvider().equals("facebook")) {
                return APIResponseUtil.badRequest("Unsupported provider: " + request.getProvider());
            }

            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            request.setIpAddress(ipAddress);
            request.setUserAgent(userAgent);

            AuthResponse response = oAuthService.processOAuth2Login(request, httpRequest);

            log.info("OAuth2 login successful for: {} via {}", request.getEmail(), request.getProvider());
            return APIResponseUtil.success(response, "OAuth2 authentication successful");

        } catch (IllegalArgumentException e) {
            log.warn("OAuth2 callback validation failed for {}: {}", request.getEmail(), e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("OAuth2 callback failed for {}: {}", request.getEmail(), e.getMessage(), e);
            return APIResponseUtil.error("OAuth2 authentication failed: " + e.getMessage());
        } finally {
            // Always remove the lock
            processingRequests.remove(requestKey);
        }
    }

    /**
     * Link OAuth2 account to existing user - NEW ENDPOINT
     */
    @PostMapping("/link")
    public ResponseEntity<APIResponse<MessageResponse>> linkOAuth2Account(
            @Valid @RequestBody OAuth2LinkRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return APIResponseUtil.unauthorized("User must be authenticated to link OAuth2 account");
            }

            String userEmail = authentication.getName();
            log.info("Attempting to link {} account for user: {}", request.getProvider(), userEmail);

            // Validate request
            if (!request.getProvider().equals("google") && !request.getProvider().equals("facebook")) {
                return APIResponseUtil.badRequest("Unsupported provider: " + request.getProvider());
            }

            MessageResponse response = oAuthService.linkOAuth2Account(userEmail, request);

            log.info("Successfully linked {} account for user: {}", request.getProvider(), userEmail);
            return APIResponseUtil.success(response, "OAuth2 account linked successfully");

        } catch (IllegalArgumentException e) {
            log.warn("OAuth2 account linking validation failed: {}", e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to link OAuth2 account: {}", e.getMessage(), e);
            return APIResponseUtil.error("Failed to link OAuth2 account: " + e.getMessage());
        }
    }

    /**
     * Unlink OAuth2 account from user - NEW ENDPOINT
     */
    @DeleteMapping("/unlink/{provider}")
    public ResponseEntity<APIResponse<MessageResponse>> unlinkOAuth2Account(
            @PathVariable String provider,
            HttpServletRequest httpRequest) {

        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return APIResponseUtil.unauthorized("User must be authenticated to unlink OAuth2 account");
            }

            String userEmail = authentication.getName();
            log.info("Attempting to unlink {} account for user: {}", provider, userEmail);

            // Validate provider
            if (!provider.equals("google") && !provider.equals("facebook")) {
                return APIResponseUtil.badRequest("Unsupported provider: " + provider);
            }

            MessageResponse response = oAuthService.unlinkOAuth2Account(userEmail, provider);

            log.info("Successfully unlinked {} account for user: {}", provider, userEmail);
            return APIResponseUtil.success(response, "OAuth2 account unlinked successfully");

        } catch (IllegalArgumentException e) {
            log.warn("OAuth2 account unlinking validation failed: {}", e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to unlink OAuth2 account: {}", e.getMessage(), e);
            return APIResponseUtil.error("Failed to unlink OAuth2 account: " + e.getMessage());
        }
    }

    /**
     * Get OAuth2 account status for current user - NEW ENDPOINT
     */
    @GetMapping("/status")
    public ResponseEntity<APIResponse<Object>> getOAuth2Status() {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return APIResponseUtil.unauthorized("User must be authenticated");
            }

            // For now, return basic status
            // You can enhance this to return actual OAuth2 account linking status
            return APIResponseUtil.success(null, "OAuth2 status retrieved");

        } catch (Exception e) {
            log.error("Failed to get OAuth2 status: {}", e.getMessage(), e);
            return APIResponseUtil.error("Failed to get OAuth2 status");
        }
    }

    // ================ UTILITY METHODS - MAINTAIN EXISTING FUNCTIONALITY ================

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);

        if ((scheme.equals("http") && serverPort != 80) ||
                (scheme.equals("https") && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }

        baseUrl.append(contextPath);
        return baseUrl.toString();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
