package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.auth.*;
import in.mapmytour.auth.entity.RefreshToken;
import in.mapmytour.auth.entity.Role;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.repository.RefreshTokenRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.service.NotificationService;
import in.mapmytour.auth.service.OAuthService;
import in.mapmytour.auth.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OAuthServiceImpl implements OAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-expiration:604800000}") // 7 days
    private long refreshTokenExpirationMs;

    // Add concurrent processing tracking to prevent duplicate processing
    private final ConcurrentHashMap<String, Boolean> processingUsers = new ConcurrentHashMap<>();

    // ================ OAUTH INTEGRATION ================

    @Override
    @Transactional
    public AuthResponse processOAuth2Login(OAuth2LoginRequest request, HttpServletRequest httpRequest) {
        String userKey = request.getEmail() + ":" + request.getProvider();

        // Check if this user is already being processed
        if (processingUsers.putIfAbsent(userKey, true) != null) {
            log.warn("OAuth2 login already processing for: {}", request.getEmail());
            throw new RuntimeException("Authentication temporarily unavailable, please try again");
        }

        long startTime = System.currentTimeMillis();
        try {
            log.info("Processing OAuth2 login for: {} via {} at {}", request.getEmail(), request.getProvider(), startTime);

            // Validate required OAuth2 request fields
            validateOAuth2Request(request);

            // Check if user exists
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            User user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                log.info("Existing user found for email: {}", request.getEmail());

                // Check for provider conflicts
                if (user.getProvider() != null &&
                        !request.getProvider().equalsIgnoreCase(user.getProvider())) {
                    throw new IllegalArgumentException(
                            "Account already exists with " + user.getProvider() + " provider. " +
                                    "Please login with " + user.getProvider()
                                    + " or contact support to link accounts.");
                }

                // Update user info from OAuth provider
                updateUserFromOAuth2(user, request);
            } else {
                // Create new user from OAuth2 data for first-time users
                log.info("Creating new user for first-time OAuth2 login: {}", request.getEmail());
                user = createUserFromOAuth2(request);
            }

            long userProcessedTime = System.currentTimeMillis();
            log.debug("User processing (lookup/create/update) took {} ms", (userProcessedTime - startTime));

            // Update last login and reset attempts
            user.setLastLoginAt(LocalDateTime.now());
            user.resetLoginAttempts();
            user = userRepository.save(user);

            // Generate session and device IDs
            String sessionId = UUID.randomUUID().toString();
            String deviceId = generateDeviceId(request.getUserAgent(), request.getIpAddress());

            // Generate tokens with optimized approach (avoiding deadlock)
            List<String> allRoles = collectAllRoles(user);
            String accessToken = jwtUtil.generateAccessToken(
                    user.getEmail(),
                    user.getId(),
                    user.getFullName(),
                    allRoles,
                    deviceId,
                    sessionId,
                    request.getIpAddress(),
                    request.getUserAgent(),
                    false);

            // Create refresh token with deadlock handling
            RefreshToken refreshTokenEntity = createRefreshTokenOptimized(user, deviceId, sessionId,
                    request.getIpAddress(), request.getUserAgent(), false);

            long tokensGeneratedTime = System.currentTimeMillis();
            log.debug("Token generation (access/refresh) took {} ms", (tokensGeneratedTime - userProcessedTime));

            UserResponse userResponse = mapToUserResponse(user);

            logSecurityEvent(user.getEmail(), "OAUTH2_LOGIN_SUCCESS",
                    request.getIpAddress(), request.getUserAgent());

            log.info("OAuth2 login successful for: {} via {}", user.getEmail(), request.getProvider());

            AuthResponse authResponse = AuthResponse.builder()
                    .isAuthenticated(true)
                    .email(user.getEmail())
                    .accessToken(accessToken)
                    .user(userResponse)
                    .refreshToken(refreshTokenEntity.getToken())
                    .expiresIn(jwtUtil.getExpirationTime() / 1000)
                    .tokenType("Bearer")
                    .sessionId(sessionId)
                    .deviceId(deviceId)
                    .requiresTwoFactor(false)
                    .twoFactorToken(null)
                    .provider(request.getProvider())
                    .build();

            long endTime = System.currentTimeMillis();
            log.info("OAuth2 login process completed in {} ms for {}", (endTime - startTime), user.getEmail());

            return authResponse;

        } catch (CannotAcquireLockException e) {
            log.error("Database deadlock detected for OAuth2 login: {}", request.getEmail());
            throw new RuntimeException("Authentication temporarily unavailable, please try again", e);
        } catch (Exception e) {
            log.error("OAuth2 login failed for email: {} via provider: {}",
                    request.getEmail(), request.getProvider(), e);
            throw new RuntimeException("OAuth2 authentication failed: " + e.getMessage(), e);
        } finally {
            // Always remove the processing lock
            processingUsers.remove(userKey);
        }
    }

    private void validateOAuth2Request(OAuth2LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required for OAuth2 login");
        }
        if (request.getProvider() == null || request.getProvider().trim().isEmpty()) {
            throw new IllegalArgumentException("Provider is required for OAuth2 login");
        }
        if (request.getProviderId() == null || request.getProviderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID is required for OAuth2 login");
        }

        // Validate provider
        if (!request.getProvider().equalsIgnoreCase("google") &&
                !request.getProvider().equalsIgnoreCase("facebook")) {
            throw new IllegalArgumentException("Unsupported provider: " + request.getProvider());
        }
    }

    private User createUserFromOAuth2(OAuth2LoginRequest request) {
        try {
            log.info("Creating new OAuth2 user with email: {}", request.getEmail());

            // Normalize email
            String email = request.getEmail().toLowerCase().trim();

            // Set default values for missing fields
            String firstName = (request.getFirstName() != null && !request.getFirstName().trim().isEmpty())
                    ? request.getFirstName().trim()
                    : "User";
            String lastName = (request.getLastName() != null && !request.getLastName().trim().isEmpty())
                    ? request.getLastName().trim()
                    : "";

            // Create user with OAuth2 data and email as password
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(email)) // Use email as password and encode it
                    .firstName(firstName)
                    .lastName(lastName)
                    .avatarUrl(request.getEffectiveAvatarUrl()) // Use effective avatar URL (handles both avatarUrl and
                                                                // profileImageUrl)
                    .provider(request.getProvider().toLowerCase())
                    .isVerified(true) // OAuth users are pre-verified
                    .isActive(true)
                    .role(User.UserRole.USER)
                    .loginAttempts(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .preferences(User.UserPreferences.builder()
                            .notifications(User.NotificationPreferences.builder()
                                    .email(true)
                                    .sms(false)
                                    .push(true)
                                    .build())
                            .privacy(User.PrivacyPreferences.builder()
                                    .profileVisible(true)
                                    .showBookingHistory(true)
                                    .build())
                            .build())
                    .build();

            // Set provider-specific IDs
            if ("google".equalsIgnoreCase(request.getProvider())) {
                user.setGoogleId(request.getProviderId());
            } else if ("facebook".equalsIgnoreCase(request.getProvider())) {
                user.setFacebookId(request.getProviderId());
            }

            // Save user to database
            user = userRepository.save(user);
            log.info("Successfully created new OAuth2 user with ID: {}", user.getId());

            // Send welcome notification asynchronously (don't fail registration if notification fails)
            try {
                log.info("New user registered via OAuth: {}", user.getEmail());
                notificationService.sendWelcomeNotification(user);
            } catch (Exception e) {
                log.warn("Failed to send welcome notification to {}: {}", user.getEmail(), e.getMessage());
                // Don't fail the registration for notification issues
            }

            return user;

        } catch (Exception e) {
            log.error("Failed to create OAuth2 user for email: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        }
    }

    private void updateUserFromOAuth2(User user, OAuth2LoginRequest request) {
        boolean hasChanges = false;

        // Update first name if provided and different
        if (request.getFirstName() != null &&
                !request.getFirstName().trim().isEmpty() &&
                !request.getFirstName().equals(user.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
            hasChanges = true;
        }

        // Update last name if provided and different
        if (request.getLastName() != null &&
                !request.getLastName().trim().isEmpty() &&
                !request.getLastName().equals(user.getLastName())) {
            user.setLastName(request.getLastName().trim());
            hasChanges = true;
        }

        // Update avatar URL if provided and different (handles both avatarUrl and
        // profileImageUrl)
        String effectiveAvatarUrl = request.getEffectiveAvatarUrl();
        if (effectiveAvatarUrl != null &&
                !effectiveAvatarUrl.trim().isEmpty() &&
                !effectiveAvatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(effectiveAvatarUrl.trim());
            hasChanges = true;
        }

        // Update provider if not set
        if (user.getProvider() == null || user.getProvider().trim().isEmpty()) {
            user.setProvider(request.getProvider().toLowerCase());
            hasChanges = true;
        }

        // Update provider-specific IDs if missing
        if ("google".equalsIgnoreCase(request.getProvider()) && user.getGoogleId() == null) {
            user.setGoogleId(request.getProviderId());
            hasChanges = true;
        } else if ("facebook".equalsIgnoreCase(request.getProvider()) && user.getFacebookId() == null) {
            user.setFacebookId(request.getProviderId());
            hasChanges = true;
        }

        // Ensure user is verified and active for OAuth login
        if (!user.getIsVerified()) {
            user.setIsVerified(true);
            hasChanges = true;
        }

        if (!user.getIsActive()) {
            user.setIsActive(true);
            hasChanges = true;
        }

        // Update password if user doesn't have one (for accounts created via
        // email/password first)
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getEmail()));
            hasChanges = true;
        }

        if (hasChanges) {
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Updated existing OAuth2 user: {}", user.getEmail());
        }
    }

    // Optimized method to avoid deadlock
    private RefreshToken createRefreshTokenOptimized(User user, String deviceId, String sessionId,
            String ipAddress, String userAgent, boolean rememberMe) {
        try {
            // Try to revoke existing tokens with retry mechanism
            revokeAllUserTokensWithRetry(user.getId());
        } catch (Exception e) {
            log.warn("Failed to revoke existing tokens for user {}, continuing with new token creation", user.getId());
            // Continue anyway - we'll clean up expired tokens later
        }

        // Create new token
        String token = jwtUtil.generateRefreshToken(user.getEmail(), deviceId, sessionId, ipAddress, userAgent,
                rememberMe);
        long expiration = rememberMe ? refreshTokenExpirationMs * 4 : refreshTokenExpirationMs;

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusNanos(expiration * 1_000_000))
                .deviceInfo(deviceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .isRevoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // Retry mechanism for token revocation
    @Retryable(value = { CannotAcquireLockException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    private void revokeAllUserTokensWithRetry(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            refreshTokenRepository.revokeAllUserTokens(user);
        }
    }

    // Fallback method if retry fails
    @Recover
    private void recoverFromTokenRevocationFailure(CannotAcquireLockException ex, String userId) {
        log.warn("Failed to revoke tokens for user {} after retries, will clean up later", userId);
        // Schedule async cleanup or let the scheduled cleanup handle it
    }

    @Override
    public MessageResponse linkOAuth2Account(String userEmail, OAuth2LinkRequest request) {
        try {
            log.info("Attempting to link {} account for user: {}", request.getProvider(), userEmail);

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validate request
            if (request.getProviderId() == null || request.getProviderId().trim().isEmpty()) {
                throw new IllegalArgumentException("Provider ID is required for linking account");
            }

            // Validate provider
            if (!request.getProvider().equalsIgnoreCase("google") &&
                    !request.getProvider().equalsIgnoreCase("facebook")) {
                throw new IllegalArgumentException("Unsupported provider: " + request.getProvider());
            }

            // Verify the OAuth2 account isn't already linked to another user
            if ("google".equalsIgnoreCase(request.getProvider())) {
                Optional<User> existingGoogleUser = userRepository.findByGoogleId(request.getProviderId());
                if (existingGoogleUser.isPresent() && !existingGoogleUser.get().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("This Google account is already linked to another user");
                }
                user.setGoogleId(request.getProviderId());
                log.info("Linked Google account {} to user {}", request.getProviderId(), userEmail);
            } else if ("facebook".equalsIgnoreCase(request.getProvider())) {
                Optional<User> existingFacebookUser = userRepository.findByFacebookId(request.getProviderId());
                if (existingFacebookUser.isPresent() && !existingFacebookUser.get().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("This Facebook account is already linked to another user");
                }
                user.setFacebookId(request.getProviderId());
                log.info("Linked Facebook account {} to user {}", request.getProviderId(), userEmail);
            }

            // Update user profile information if provided
            boolean hasProfileChanges = false;
            if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty() &&
                    !request.getFirstName().equals(user.getFirstName())) {
                user.setFirstName(request.getFirstName().trim());
                hasProfileChanges = true;
            }

            if (request.getLastName() != null && !request.getLastName().trim().isEmpty() &&
                    !request.getLastName().equals(user.getLastName())) {
                user.setLastName(request.getLastName().trim());
                hasProfileChanges = true;
            }

            // Update avatar URL if provided and different (handles both avatarUrl and
            // profileImageUrl)
            String effectiveAvatarUrl = request.getEffectiveAvatarUrl();
            if (effectiveAvatarUrl != null && !effectiveAvatarUrl.trim().isEmpty() &&
                    !effectiveAvatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(effectiveAvatarUrl.trim());
                hasProfileChanges = true;
            }

            if (hasProfileChanges) {
                log.info("Updated profile information for user {} during OAuth2 linking", userEmail);
            }

            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            logSecurityEvent(userEmail, "OAUTH2_ACCOUNT_LINKED", null, null);

            return MessageResponse.builder()
                    .message("OAuth2 account linked successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to link OAuth2 account for {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to link OAuth2 account: " + e.getMessage());
        }
    }

    @Override
    public MessageResponse unlinkOAuth2Account(String userEmail, String provider) {
        try {
            log.info("Attempting to unlink {} account for user: {}", provider, userEmail);

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validate provider
            if (!provider.equalsIgnoreCase("google") && !provider.equalsIgnoreCase("facebook")) {
                throw new IllegalArgumentException("Unsupported provider: " + provider);
            }

            if ("google".equalsIgnoreCase(provider)) {
                if (user.getGoogleId() == null) {
                    throw new IllegalArgumentException("Google account is not linked");
                }
                user.setGoogleId(null);
                log.info("Unlinked Google account for user: {}", userEmail);
            } else if ("facebook".equalsIgnoreCase(provider)) {
                if (user.getFacebookId() == null) {
                    throw new IllegalArgumentException("Facebook account is not linked");
                }
                user.setFacebookId(null);
                log.info("Unlinked Facebook account for user: {}", userEmail);
            }

            // If this was the primary provider and user has no password, require them to
            // set one
            if (provider.equalsIgnoreCase(user.getProvider()) &&
                    (user.getPassword() == null || user.getPassword().isEmpty())) {
                throw new IllegalArgumentException(
                        "Cannot unlink primary OAuth2 provider without setting a password first");
            }

            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            logSecurityEvent(userEmail, "OAUTH2_ACCOUNT_UNLINKED", null, null);

            return MessageResponse.builder()
                    .message("OAuth2 account unlinked successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to unlink OAuth2 account for {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to unlink OAuth2 account: " + e.getMessage());
        }
    }

    // ================ UTILITY METHODS ================

    private String generateDeviceId(String userAgent, String ipAddress) {
        String deviceString = (userAgent != null ? userAgent : "unknown") +
                (ipAddress != null ? ipAddress : "unknown");
        return UUID.nameUUIDFromBytes(deviceString.getBytes()).toString();
    }

    private UserResponse mapToUserResponse(User user) {
        String primaryRole = user.getRole().name().toLowerCase();
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            java.util.List<String> rbacRoles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(java.util.stream.Collectors.toList());
            java.util.List<String> hierarchy = java.util.Arrays.asList("SUPER_ADMIN", "ADMIN", "MANAGER", "EMPLOYEE",
                    "B2B", "USER");
            boolean found = false;
            for (String role : hierarchy) {
                if (rbacRoles.stream().anyMatch(r -> r.replaceFirst("^ROLE_", "").equalsIgnoreCase(role))) {
                    primaryRole = role.toLowerCase();
                    found = true;
                    break;
                }
            }
            if (!found && !rbacRoles.isEmpty()) {
                primaryRole = rbacRoles.get(0).replaceFirst("^ROLE_", "").toLowerCase();
            }
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .gender(user.getGender() != null ? user.getGender().name().toLowerCase() : null)
                .role(primaryRole)
                .isVerified(user.getIsVerified())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null)
            return "unknown";

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

    private String extractDeviceInfo(HttpServletRequest request) {
        if (request == null)
            return "Unknown Device";

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null)
            return "Unknown Device";

        if (userAgent.contains("iPhone"))
            return "iPhone";
        if (userAgent.contains("iPad"))
            return "iPad";
        if (userAgent.contains("Android"))
            return "Android Device";
        if (userAgent.contains("Windows"))
            return "Windows PC";
        if (userAgent.contains("Macintosh"))
            return "Mac";
        if (userAgent.contains("Linux"))
            return "Linux PC";

        return "Unknown Device";
    }

    private void logSecurityEvent(String userEmail, String eventType, String ipAddress, String userAgent) {
        // Implementation for logging security events
        log.info("Security Event - User: {}, Event: {}, IP: {}, UserAgent: {}",
                userEmail, eventType, ipAddress, userAgent);

        // Here you can add additional logging to database or external systems
        // Example: securityLogRepository.save(SecurityLog.builder()...build());
    }

    /**
     * Collect all roles from a user (both UserRole enum and Role entities)
     */
    private java.util.List<String> collectAllRoles(User user) {
        java.util.List<String> roles = new java.util.ArrayList<>();

        // Add the primary UserRole enum value (admin, user, etc.)
        if (user.getRole() != null) {
            String primaryRole = user.getRole().name().toUpperCase();
            roles.add(primaryRole);
            // Also add with ROLE_ prefix for Spring Security compatibility
            roles.add("ROLE_" + primaryRole);
        }

        // Also add any additional Role entities if they exist
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.getRoles().forEach(role -> {
                if (role.getName() != null && !roles.contains(role.getName())) {
                    roles.add(role.getName());
                    // Add with ROLE_ prefix if not already present
                    if (!role.getName().startsWith("ROLE_")) {
                        roles.add("ROLE_" + role.getName());
                    }
                }
            });
        }

        return roles;
    }
}
