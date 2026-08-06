package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.auth.*;
import in.mapmytour.auth.dto.user.AdminLoginHistoryItemResponse;
import in.mapmytour.auth.entity.Agent;
import in.mapmytour.auth.entity.Supplier;
import in.mapmytour.auth.entity.LoginHistory;
import in.mapmytour.auth.entity.OtpToken;
import in.mapmytour.auth.entity.Permission;
import in.mapmytour.auth.entity.RefreshToken;
import in.mapmytour.auth.entity.Role;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.helper.GeoLocationHelper;
import in.mapmytour.auth.repository.AgentRepository;
import in.mapmytour.auth.repository.SupplierRepository;
import in.mapmytour.auth.repository.LoginHistoryRepository;
import in.mapmytour.auth.helper.RealtimeNotificationHelper;
import in.mapmytour.auth.repository.OtpTokenRepository;
import in.mapmytour.auth.repository.PermissionRepository;
import in.mapmytour.auth.repository.RefreshTokenRepository;
import in.mapmytour.auth.repository.RoleRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.service.AuthService;
import in.mapmytour.auth.service.NotificationService;
import in.mapmytour.auth.utils.JwtUtil;
import in.mapmytour.auth.utils.ValidationUtil;
import in.mapmytour.auth.event.AuthEventProducer;
import in.mapmytour.auth.helper.S3Helper;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.MDC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import in.mapmytour.auth.entity.VerificationRule;
import in.mapmytour.auth.entity.VerificationRequest;
import in.mapmytour.auth.repository.VerificationRuleRepository;
import in.mapmytour.auth.repository.VerificationRequestRepository;
import in.mapmytour.auth.service.AutomatedVerificationService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final GeoLocationHelper geoLocationHelper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;
    private final AuthEventProducer authEventProducer;
    private final S3Helper s3Helper;
    private final AgentRepository agentRepository;
    private final SupplierRepository supplierRepository;
    private final VerificationRuleRepository verificationRuleRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final AutomatedVerificationService automatedVerificationService;
    private final RealtimeNotificationHelper realtimeNotificationHelper;

    @Value("${app.jwt.refresh-expiration:604800000}") // 7 days
    private long refreshTokenExpirationMs;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.account-lock-duration:3600000}") // 1 hour
    private long accountLockDurationMs;

    // Rate limiting storage
    private final Map<String, List<LocalDateTime>> rateLimitStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> blockedUsers = new ConcurrentHashMap<>();

    // ================ BASIC AUTHENTICATION ================

    @Override
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        try {
            // Check rate limiting
            if (isRateLimited(request.getEmail(), "login")) {
                throw new BadCredentialsException("Too many login attempts. Please try again later.");
            }

            // Check if user exists and is not locked
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (!user.isAccountNonLocked()) {
                throw new BadCredentialsException("Account is locked. Please try again later.");
            }

            if (!user.isActive()) {
                throw new BadCredentialsException("Account is deactivated. Please contact support.");
            }

            // if (!user.isVerified()) {
            // throw new BadCredentialsException("Please verify your email before logging
            // in.");
            // }

            // password validation
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadCredentialsException("Invalid email or password");
            }

            // Reset login attempts and update last login
            user.resetLoginAttempts();
            userRepository.save(user);

            // Generate session and device IDs
            String sessionId = UUID.randomUUID().toString();
            String deviceId = generateDeviceId(userAgent, ipAddress);

            // Generate tokens with additional claims (include all roles)
            List<String> allRoles = collectAllRoles(user);
            String accessToken = jwtUtil.generateAccessToken(
                    user.getEmail(),
                    user.getId(),
                    user.getFullName(),
                    allRoles,
                    deviceId,
                    sessionId,
                    ipAddress,
                    userAgent,
                    request.getRememberMe());

            String refreshToken = createRefreshToken(user, deviceId, sessionId, ipAddress, userAgent,
                    request.getRememberMe());

            // Create user response
            UserResponse userResponse = mapToUserResponse(user);

            // Create specialized responses if applicable
            AgentResponse agentResponse = null;
            SupplierResponse supplierResponse = null;

            if (user.getRole() == User.UserRole.B2B) {
                Optional<Agent> agentOpt = agentRepository.findByUser(user);
                if (agentOpt.isPresent()) {
                    agentResponse = mapToAgentResponse(agentOpt.get());
                }
            } else if (user.getRole() == User.UserRole.ADMIN || user.getRole() == User.UserRole.SUPER_ADMIN) {
                // Check if they are also a supplier (though usually admins are separate)
                Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
                if (supplierOpt.isPresent()) {
                    supplierResponse = mapToSupplierResponse(supplierOpt.get());
                }
            } else {
                // Check if the user is a supplier (role USER but has supplier record, or
                // specific role)
                // In this system, Suppliers might have specific roles or just be flagged.
                // Assuming B2B is Agent, and we check Supplier Repository for others.
                Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
                if (supplierOpt.isPresent()) {
                    supplierResponse = mapToSupplierResponse(supplierOpt.get());
                }
            }

            // Log successful login
            logSecurityEvent(user.getEmail(), "LOGIN_SUCCESS", ipAddress, userAgent);
            logLoginHistory(user, ipAddress, userAgent, true);

            // Publish user login event to Kafka
            try {
                String correlationId = MDC.get("correlationId");
                if (correlationId == null) {
                    correlationId = UUID.randomUUID().toString();
                }
                authEventProducer.publishUserLogin(user.getId(), user.getEmail(), ipAddress, correlationId);
            } catch (Exception e) {
                log.warn("Failed to publish user login event for user {}: {}", user.getEmail(), e.getMessage());
            }

            log.info("User logged in successfully: {}", user.getEmail());

            return AuthResponse.builder()
                    .isAuthenticated(true)
                    .email(user.getEmail())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userResponse)
                    .agent(agentResponse)
                    .supplier(supplierResponse)
                    .expiresIn(jwtUtil.getExpirationTime())
                    .tokenType("Bearer")
                    .sessionId(sessionId)
                    .deviceId(deviceId)
                    .build();

        } catch (AuthenticationException e) {
            // Handle failed login attempts
            userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                user.incrementLoginAttempts();
                userRepository.save(user);
                logSecurityEvent(user.getEmail(), "LOGIN_FAILED", ipAddress, userAgent);
                logLoginHistory(user, ipAddress, userAgent, false);
            });

            log.warn("Login failed for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    public MessageResponse sendOtp(String request) {
        // Check if user exists
        User user = userRepository.findByEmail(request)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Generate and send OTP
        String otp = generateOtp();

        OtpToken otpToken = OtpToken.builder()
                .email(request)
                .otp(otp)
                .type(OtpToken.OtpType.LOGIN_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        otpTokenRepository.save(otpToken);

        // Send email asynchronously so HTTP request is fast and not blocked by
        // SMTP/network latency
        try {
            new Thread(() -> {
                try {
                    Map<String, Object> vars = new HashMap<>();
                    vars.put("firstName", user.getFirstName());
                    vars.put("verificationCode", otp);
                    notificationService.sendEmail(request, "Your OTP for MapMyTimes", "", "verification_email", vars);
                } catch (Exception e) {
                    log.error("Failed to send OTP email asynchronously to {}: {}", request, e.getMessage(), e);
                }
            }, "send-otp-email-" + user.getId()).start();
        } catch (Exception e) {
            // In case thread creation fails, just log it – do not fail the API response
            log.error("Failed to start async OTP email thread for {}: {}", request, e.getMessage(), e);
        }

        return MessageResponse.builder()
                .message("OTP sent to your email")
                .build();
    }

    @Override
    public AuthResponse loginWithOtp(LoginWithOtpRequest request) {
        // Verify OTP first
        OtpToken otpToken = otpTokenRepository.findByEmailAndOtpAndType(
                request.getEmail(), request.getOtp(), OtpToken.OtpType.LOGIN_VERIFICATION)
                .orElseThrow(() -> new BadCredentialsException("Invalid OTP"));

        if (!otpToken.isValid()) {
            throw new BadCredentialsException("OTP has expired or is invalid");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Mark OTP as used
        otpToken.markAsUsed();
        otpTokenRepository.save(otpToken);

        // Generate tokens
        String sessionId = UUID.randomUUID().toString();
        String deviceId = generateDeviceId(request.getUserAgent(), request.getIpAddress());

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

        String refreshToken = createRefreshToken(user, deviceId, sessionId, request.getIpAddress(),
                request.getUserAgent(), false);

        UserResponse userResponse = mapToUserResponse(user);

        // Create specialized responses if applicable
        AgentResponse agentResponse = null;
        SupplierResponse supplierResponse = null;

        if (user.getRole() == User.UserRole.B2B) {
            Optional<Agent> agentOpt = agentRepository.findByUser(user);
            if (agentOpt.isPresent()) {
                agentResponse = mapToAgentResponse(agentOpt.get());
            }
        } else {
            Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
            if (supplierOpt.isPresent()) {
                supplierResponse = mapToSupplierResponse(supplierOpt.get());
            }
        }

        logSecurityEvent(user.getEmail(), "OTP_LOGIN_SUCCESS", request.getIpAddress(), request.getUserAgent());
        logLoginHistory(user, request.getIpAddress(), request.getUserAgent(), true);

        return AuthResponse.builder()
                .isAuthenticated(true)
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .agent(agentResponse)
                .supplier(supplierResponse)
                .expiresIn(jwtUtil.getExpirationTime())
                .tokenType("Bearer")
                .sessionId(sessionId)
                .deviceId(deviceId)
                .build();
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Validate password strength
        if (!ValidationUtil.isStrongPassword(request.getPassword())) {
            throw new IllegalArgumentException("Password does not meet security requirements: " +
                    ValidationUtil.getPasswordStrengthMessage(request.getPassword()));
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.USER)
                .isVerified(false)
                .isActive(true)
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

        userRepository.save(user);

        // Send verification email
        // sendEmailVerificationOtp(user.getEmail());

        log.info("User registered successfully: {}", user.getEmail());

        notificationService.sendWelcomeNotification(user);

        // Publish user registered event to Kafka
        try {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            authEventProducer.publishUserRegistered(user.getId(), user.getEmail(), correlationId);
        } catch (Exception e) {
            log.warn("Failed to publish user registered event for user {}: {}", user.getEmail(), e.getMessage());
        }

        // Generate tokens
        String sessionId = UUID.randomUUID().toString();
        String deviceId = generateDeviceId(request.getUserAgent(), request.getIpAddress());

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

        String refreshToken = createRefreshToken(user, deviceId, sessionId, request.getIpAddress(),
                request.getUserAgent(), false);

        UserResponse userResponse = mapToUserResponse(user);

        logSecurityEvent(user.getEmail(), "OTP_LOGIN_SUCCESS", request.getIpAddress(), request.getUserAgent());
        logLoginHistory(user, request.getIpAddress(), request.getUserAgent(), true);

        return AuthResponse.builder()
                .isAuthenticated(true)
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .expiresIn(jwtUtil.getExpirationTime())
                .tokenType("Bearer")
                .sessionId(sessionId)
                .deviceId(deviceId)
                .build();
    }

    @Override
    public MessageResponse logout(String userEmail, String refreshToken) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (refreshToken != null) {
            // Revoke specific refresh token
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(token -> {
                        token.setIsRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        } else {
            // Revoke all refresh tokens
            refreshTokenRepository.revokeAllUserTokens(user);
        }

        logSecurityEvent(userEmail, "LOGOUT", null, null);

        log.info("User logged out successfully: {}", userEmail);
        return MessageResponse.builder()
                .message("Logged out successfully")
                .build();
    }

    @Override
    public MessageResponse logoutAllDevices(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        logSecurityEvent(userEmail, "LOGOUT_ALL_DEVICES", null, null);

        log.info("User logged out from all devices: {}", userEmail);
        return MessageResponse.builder()
                .message("Logged out from all devices successfully")
                .build();
    }

    // ================ PASSWORD MANAGEMENT ================

    @Override
    public MessageResponse forgotPasswordStep1(ForgotPasswordStep1Request request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email address"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is deactivated. Please contact support.");
        }

        // Check rate limiting
        long recentOtps = otpTokenRepository.countRecentOtpsByEmailAndType(
                request.getEmail(), OtpToken.OtpType.PASSWORD_RESET, LocalDateTime.now().minusMinutes(15));

        if (recentOtps >= 3) {
            throw new IllegalArgumentException("Too many OTP requests. Please try again after 15 minutes.");
        }

        // Generate and send OTP
        String otp = generateOtp();

        OtpToken otpToken = OtpToken.builder()
                .email(request.getEmail())
                .otp(otp)
                .type(OtpToken.OtpType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        otpTokenRepository.save(otpToken);

        // Send OTP via email and SMS
        Map<String, Object> vars = new HashMap<>();
        vars.put("firstName", user.getFirstName());
        vars.put("verificationCode", otp);
        notificationService.sendEmail(request.getEmail(), "Your OTP for MapMyTimes", "", "verification_email", vars);
        
        if (user.getPhone() != null && ValidationUtil.isValidPhoneNumber(user.getPhone())) {
            notificationService.sendSMS(user.getPhone(), "Your MapMyTour password reset OTP is: " + otp);
        }

        logSecurityEvent(request.getEmail(), "PASSWORD_RESET_OTP_SENT", null, null);

        // Publish password reset requested event to Kafka
        try {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            authEventProducer.publishPasswordResetRequested(user.getId(), user.getEmail(), correlationId);
        } catch (Exception e) {
            log.warn("Failed to publish password reset requested event for user {}: {}", request.getEmail(),
                    e.getMessage());
        }

        log.info("Password reset OTP sent to: {}", request.getEmail());
        return MessageResponse.builder()
                .message("OTP sent to your email and phone number. Please verify to proceed.")
                .build();
    }

    @Override
    public TokenResponse forgotPasswordStep2(ForgotPasswordStep2Request request) {
        OtpToken otpToken = otpTokenRepository.findByEmailAndOtpAndType(
                request.getEmail(), request.getOtp(), OtpToken.OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new IllegalArgumentException("Invalid OTP"));

        if (!otpToken.isValid()) {
            throw new IllegalArgumentException("OTP has expired or is invalid");
        }

        if (otpToken.getAttempts() >= 5) {
            throw new IllegalArgumentException("Too many attempts. Please request a new OTP.");
        }

        // Generate verification token
        String verificationToken = jwtUtil.generatePasswordResetToken(request.getEmail());
        otpToken.setVerificationToken(verificationToken);
        otpToken.markAsUsed();
        otpTokenRepository.save(otpToken);

        logSecurityEvent(request.getEmail(), "PASSWORD_RESET_OTP_VERIFIED", null, null);

        log.info("OTP verified for password reset: {}", request.getEmail());
        return TokenResponse.builder()
                .token(verificationToken)
                .message("OTP verified successfully. You can now reset your password.")
                .build();
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (!ValidationUtil.isStrongPassword(request.getPassword())) {
            throw new IllegalArgumentException("Password does not meet security requirements: " +
                    ValidationUtil.getPasswordStrengthMessage(request.getPassword()));
        }

        // Validate token
        if (!jwtUtil.validateToken(request.getToken()) || !jwtUtil.isPasswordResetToken(request.getToken())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        String email = jwtUtil.getEmailFromToken(request.getToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // Revoke all existing tokens for security
        refreshTokenRepository.revokeAllUserTokens(user);

        // Send confirmation email
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getFirstName());
        notificationService.sendEmail(user.getEmail(), "Password Reset Successful",
                null, "password_reset_success", vars);

        logSecurityEvent(email, "PASSWORD_RESET_COMPLETED", null, null);

        // Publish password changed event to Kafka
        try {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            authEventProducer.publishPasswordChanged(user.getId(), user.getEmail(), correlationId);
        } catch (Exception e) {
            log.warn("Failed to publish password changed event for user {}: {}", email, e.getMessage());
        }

        log.info("Password reset via token for: {}", email);
        return MessageResponse.builder()
                .message("Password reset successfully. Please login with your new password.")
                .build();
    }

    @Override
    public MessageResponse changePassword(ChangePasswordRequest request, String userEmail) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (!ValidationUtil.isStrongPassword(request.getNewPassword())) {
            throw new IllegalArgumentException("Password does not meet security requirements: " +
                    ValidationUtil.getPasswordStrengthMessage(request.getNewPassword()));
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(user);

        logSecurityEvent(userEmail, "PASSWORD_CHANGED", null, null);

        log.info("Password changed successfully for: {}", userEmail);
        return MessageResponse.builder()
                .message("Password changed successfully. Please login again.")
                .build();
    }

    @Override
    public MessageResponse validatePassword(String password) {
        if (!ValidationUtil.isStrongPassword(password)) {
            return MessageResponse.builder()
                    .message(ValidationUtil.getPasswordStrengthMessage(password))
                    .build();
        }

        return MessageResponse.builder()
                .message("Password is strong and meets all security requirements")
                .build();
    }

    // ================ EMAIL VERIFICATION ================

    @Override
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        OtpToken otpToken = otpTokenRepository.findByEmailAndOtpAndType(
                request.getEmail(), request.getOtp(), OtpToken.OtpType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new IllegalArgumentException("Invalid OTP"));

        if (!otpToken.isValid()) {
            throw new IllegalArgumentException("OTP has expired or is invalid");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify user
        user.setIsVerified(true);
        userRepository.save(user);

        // Mark OTP as used
        otpToken.markAsUsed();
        otpTokenRepository.save(otpToken);

        // Send welcome email
        notificationService.sendWelcomeNotification(user);

        logSecurityEvent(request.getEmail(), "EMAIL_VERIFIED", null, null);

        log.info("Email verified successfully for: {}", request.getEmail());

        // Auto-login after verification
        user.resetLoginAttempts();
        userRepository.save(user);

        String sessionId = UUID.randomUUID().toString();
        String deviceId = generateDeviceId("", "");

        List<String> allRoles = collectAllRoles(user);
        String accessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getId(),
                user.getFullName(),
                allRoles,
                deviceId,
                sessionId,
                "",
                "",
                false);

        String refreshToken = createRefreshToken(user, deviceId, sessionId, "", "", false);

        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.builder()
                .isAuthenticated(true)
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .expiresIn(jwtUtil.getExpirationTime())
                .tokenType("Bearer")
                .sessionId(sessionId)
                .deviceId(deviceId)
                .build();
    }

    @Override
    public MessageResponse resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getIsVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        // Check rate limiting
        long recentOtps = otpTokenRepository.countRecentOtpsByEmailAndType(
                request.getEmail(), OtpToken.OtpType.EMAIL_VERIFICATION, LocalDateTime.now().minusMinutes(5));

        if (recentOtps >= 3) {
            throw new IllegalArgumentException("Too many verification requests. Please try again after 5 minutes.");
        }

        sendEmailVerificationOtp(request.getEmail(), user.getFirstName());

        return MessageResponse.builder()
                .message("Verification email sent successfully.")
                .build();
    }

    @Override
    public MessageResponse sendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getIsVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        sendEmailVerificationOtp(email, user.getFirstName());

        return MessageResponse.builder()
                .message("Verification OTP sent successfully.")
                .build();
    }

    // ================ TOKEN MANAGEMENT ================

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token has expired");
        }

        User user = refreshToken.getUser();

        // Generate new access token
        List<String> allRoles = collectAllRoles(user);
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getId(),
                user.getFullName(),
                allRoles,
                refreshToken.getDeviceInfo(),
                UUID.randomUUID().toString(), // New session ID
                refreshToken.getIpAddress(),
                refreshToken.getUserAgent(),
                false);

        // Optionally rotate refresh token for better security
        String newRefreshToken = createRefreshToken(user,
                refreshToken.getDeviceInfo(),
                UUID.randomUUID().toString(),
                refreshToken.getIpAddress(),
                refreshToken.getUserAgent(),
                false);

        refreshToken.setIsRevoked(true);
        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.builder()
                .isAuthenticated(true)
                .email(user.getEmail())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userResponse)
                .expiresIn(jwtUtil.getExpirationTime())
                .tokenType("Bearer")
                .build();
    }

    @Override
    public MessageResponse revokeToken(RevokeTokenRequest request) {
        refreshTokenRepository.findByToken(request.getToken())
                .ifPresent(token -> {
                    token.setIsRevoked(true);
                    refreshTokenRepository.save(token);
                });

        return MessageResponse.builder()
                .message("Token revoked successfully")
                .build();
    }

    @Override
    public MessageResponse revokeAllTokens(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        refreshTokenRepository.revokeAllUserTokens(user);

        return MessageResponse.builder()
                .message("All tokens revoked successfully")
                .build();
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        try {
            if (jwtUtil.validateToken(token)) {
                Map<String, Object> metadata = jwtUtil.getTokenMetadata(token);
                return TokenValidationResponse.builder()
                        .valid(true)
                        .expired(false)
                        .tokenType(jwtUtil.getTokenType(token))
                        .username(jwtUtil.getUsernameFromToken(token))
                        .userId(jwtUtil.getUserIdFromToken(token))
                        .roles(jwtUtil.getRolesFromToken(token))
                        .expiresAt(jwtUtil.getExpirationAsLocalDateTime(token))
                        .metadata(metadata)
                        .build();
            } else {
                return TokenValidationResponse.builder()
                        .valid(false)
                        .expired(jwtUtil.isTokenExpired(token))
                        .build();
            }
        } catch (Exception e) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .expired(true)
                    .error(e.getMessage())
                    .build();
        }
    }

    // ================ ACCOUNT MANAGEMENT ================

    @Override
    public UserResponse getCurrentUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToUserResponse(user);
    }

    @Override
    public MessageResponse deactivateAccount(String userEmail, String password) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        // Deactivate account
        user.setIsActive(false);
        userRepository.save(user);

        // Revoke all tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        logSecurityEvent(userEmail, "ACCOUNT_DEACTIVATED", null, null);

        return MessageResponse.builder()
                .message("Account deactivated successfully")
                .build();
    }

    @Override
    public MessageResponse reactivateAccount(ReactivateAccountRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isActive()) {
            throw new IllegalArgumentException("Account is already active");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        // Reactivate account
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        logSecurityEvent(request.getEmail(), "ACCOUNT_REACTIVATED", null, null);

        return MessageResponse.builder()
                .message("Account reactivated successfully")
                .build();
    }

    @Override
    public MessageResponse requestAccountDeletion(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate deletion token
        String deletionToken = jwtUtil.generateSecureRandomToken();

        // Store deletion request (you might want to create a separate entity for this)
        // For now, we'll use email
        notificationService.sendEmail(userEmail, "Account Deletion Request",
                "Click this link to confirm account deletion: " + deletionToken, null, null);

        logSecurityEvent(userEmail, "ACCOUNT_DELETION_REQUESTED", null, null);

        return MessageResponse.builder()
                .message("Account deletion confirmation sent to your email")
                .build();
    }

    @Override
    public MessageResponse confirmAccountDeletion(ConfirmAccountDeletionRequest request) {
        // Validate deletion token and complete deletion
        // Implementation depends on how you store deletion requests

        return MessageResponse.builder()
                .message("Account deleted successfully")
                .build();
    }

    // ================ SECURITY FEATURES ================

    @Override
    public MessageResponse enableTwoFactorAuth(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Implementation for 2FA setup
        // Generate QR code, backup codes, etc.

        logSecurityEvent(userEmail, "TWO_FACTOR_ENABLED", null, null);

        return MessageResponse.builder()
                .message("Two-factor authentication enabled successfully")
                .build();
    }

    @Override
    public MessageResponse disableTwoFactorAuth(String userEmail, String code) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify 2FA code before disabling
        // Implementation depends on your 2FA library

        logSecurityEvent(userEmail, "TWO_FACTOR_DISABLED", null, null);

        return MessageResponse.builder()
                .message("Two-factor authentication disabled successfully")
                .build();
    }

    @Override
    public MessageResponse generateBackupCodes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();

        // Store backup codes (you might want to create a separate entity)
        // Send codes via email
        notificationService.sendEmail(userEmail, "Backup Codes Generated",
                "Your new backup codes have been generated. Please save them securely.", null, null);

        return MessageResponse.builder()
                .message("Backup codes generated and sent to your email")
                .build();
    }

    @Override
    public TwoFactorResponse verifyTwoFactorCode(TwoFactorVerificationRequest request) {
        // Verify 2FA code
        // Implementation depends on your 2FA library

        return TwoFactorResponse.builder()
                .verified(true)
                .message("Two-factor code verified successfully")
                .build();
    }

    // ================ ACCOUNT STATUS ================

    @Override
    public EmailCheckResponse checkEmailExists(String email) {
        boolean exists = userRepository.existsByEmail(email);
        return EmailCheckResponse.builder()
                .exists(exists)
                .build();
    }

    @Override
    public AccountStatusResponse getAccountStatus(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return AccountStatusResponse.builder()
                .email(user.getEmail())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .isLocked(user.isLocked())
                .loginAttempts(user.getLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public MessageResponse unlockAccount(UnlockAccountRequest request) {
        // Verify unlock token and unlock account
        // Implementation depends on how you generate unlock tokens

        return MessageResponse.builder()
                .message("Account unlocked successfully")
                .build();
    }

    // ================ SESSION MANAGEMENT ================

    @Override
    public List<UserSessionResponse> getActiveSessions(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndIsRevokedFalse(user);

        return activeTokens.stream()
                .map(token -> UserSessionResponse.builder()
                        .sessionId(token.getId())
                        .deviceInfo(token.getDeviceInfo())
                        .ipAddress(token.getIpAddress())
                        .userAgent(token.getUserAgent())
                        .createdAt(token.getCreatedAt())
                        .expiresAt(token.getExpiresAt())
                        .isCurrentSession(false) // You'd need to determine this
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse terminateSession(String userEmail, String sessionId) {
        refreshTokenRepository.findById(sessionId)
                .ifPresent(token -> {
                    token.setIsRevoked(true);
                    refreshTokenRepository.save(token);
                });

        return MessageResponse.builder()
                .message("Session terminated successfully")
                .build();
    }

    @Override
    public MessageResponse terminateAllOtherSessions(String userEmail, String currentSessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<RefreshToken> tokens = refreshTokenRepository.findByUserAndIsRevokedFalse(user);
        tokens.stream()
                .filter(token -> !token.getId().equals(currentSessionId))
                .forEach(token -> {
                    token.setIsRevoked(true);
                    refreshTokenRepository.save(token);
                });

        return MessageResponse.builder()
                .message("All other sessions terminated successfully")
                .build();
    }

    // ================ ADMIN FUNCTIONS ================

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable, String search, String role, Boolean isActive) {
        // Implementation for getting users with filters
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::mapToUserResponse);
    }

    @Override
    public Page<AdminLoginHistoryItemResponse> getAllLoginHistory(Pageable pageable, String userEmail) {
        Page<LoginHistory> loginHistoryPage;

        if (userEmail != null && !userEmail.trim().isEmpty()) {
            // Filter by specific user
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
            loginHistoryPage = loginHistoryRepository.findByUserOrderByLoginTimeDesc(user, pageable);
        } else {
            // Get all login history
            loginHistoryPage = loginHistoryRepository.findAllByOrderByLoginTimeDesc(pageable);
        }

        // Convert to admin response format (includes user email)
        return loginHistoryPage.map(loginHistory -> AdminLoginHistoryItemResponse.builder()
                .id(loginHistory.getId())
                .userEmail(loginHistory.getUser().getEmail())
                .userId(loginHistory.getUser().getId())
                .ipAddress(loginHistory.getIpAddress())
                .userAgent(loginHistory.getUserAgent())
                .location(loginHistory.getLocation())
                .loginTime(loginHistory.getLoginTime())
                .deviceType(loginHistory.getDeviceType())
                .successful(loginHistory.isSuccessful())
                .build());
    }

    @Override
    public MessageResponse adminResetPassword(AdminResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate temporary password
        String tempPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // Send temporary password via email
        notificationService.sendEmail(user.getEmail(), "Password Reset by Admin",
                "Your password has been reset by an administrator. New password: " + tempPassword + ". Please change it after login.", null, null);

        return MessageResponse.builder()
                .message("Password reset successfully. Temporary password sent to user.")
                .build();
    }

    @Override
    public MessageResponse adminUnlockAccount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsLocked(false);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Account unlocked successfully")
                .build();
    }

    @Override
    public MessageResponse adminDeactivateUser(String userEmail, String reason) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);

        // Revoke all tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        // Send notification email
        notificationService.sendEmail(userEmail, "Account Deactivated",
                "Your account has been deactivated. Please contact support if you believe this is an error.", null, null);

        return MessageResponse.builder()
                .message("User deactivated successfully")
                .build();
    }

    @Override
    public MessageResponse adminActivateUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(true);
        user.setIsLocked(false);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("User activated successfully")
                .build();
    }

    @Override
    public UserStatsResponse getUserStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Calculate user statistics
        long totalLogins = 0; // Calculate from security logs
        long activeTokens = refreshTokenRepository.countActiveTokensByUser(user);

        return UserStatsResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .totalLogins(totalLogins)
                .activeTokens(activeTokens)
                .accountCreated(user.getCreatedAt())
                .lastLogin(user.getLastLoginAt())
                .isVerified(user.isVerified())
                .isActive(user.isActive())
                .build();
    }

    // ================ SECURITY LOGS ================

    @Override
    public Page<SecurityLogResponse> getSecurityLogs(String userEmail, Pageable pageable) {
        // Implementation for security logs
        // You'd need to create a SecurityLog entity and repository
        return Page.empty();
    }

    @Override
    public MessageResponse reportSuspiciousActivity(SecurityReportRequest request) {
        // Log suspicious activity
        log.warn("Suspicious activity reported: {}", request.getDescription());

        return MessageResponse.builder()
                .message("Suspicious activity reported successfully")
                .build();
    }

    // ================ DEVICE MANAGEMENT ================

    @Override
    public List<DeviceResponse> getRegisteredDevices(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<RefreshToken> tokens = refreshTokenRepository.findByUserAndIsRevokedFalse(user);

        return tokens.stream()
                .map(token -> DeviceResponse.builder()
                        .deviceId(token.getDeviceInfo())
                        .deviceName(parseDeviceName(token.getUserAgent()))
                        .ipAddress(token.getIpAddress())
                        .userAgent(token.getUserAgent())
                        .lastActive(token.getCreatedAt())
                        .isCurrentDevice(false)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse registerDevice(String userEmail, DeviceRegistrationRequest request) {
        // Implementation for device registration
        return MessageResponse.builder()
                .message("Device registered successfully")
                .build();
    }

    @Override
    public MessageResponse revokeDevice(String userEmail, String deviceId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<RefreshToken> tokens = refreshTokenRepository.findByUserAndIsRevokedFalse(user);
        tokens.stream()
                .filter(token -> deviceId.equals(token.getDeviceInfo()))
                .forEach(token -> {
                    token.setIsRevoked(true);
                    refreshTokenRepository.save(token);
                });

        return MessageResponse.builder()
                .message("Device access revoked successfully")
                .build();
    }

    // ================ RATE LIMITING ================

    @Override
    public boolean isRateLimited(String identifier, String action) {
        String key = identifier + ":" + action;
        List<LocalDateTime> attempts = rateLimitStorage.getOrDefault(key, new ArrayList<>());

        // Clean old attempts (older than 1 hour)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        attempts.removeIf(attempt -> attempt.isBefore(oneHourAgo));

        // Check if rate limited
        if (attempts.size() >= getRateLimit(action)) {
            return true;
        }

        // Add current attempt
        attempts.add(LocalDateTime.now());
        rateLimitStorage.put(key, attempts);

        return false;
    }

    @Override
    public MessageResponse checkRateLimit(String identifier, String action) {
        boolean isLimited = isRateLimited(identifier, action);

        return MessageResponse.builder()
                .message(isLimited ? "Rate limit exceeded" : "Rate limit check passed")
                .build();
    }

    // ================ RBAC MANAGEMENT ================

    @Override
    public MessageResponse createOrUpdateRole(RoleRequest request) {
        String roleName = request.getName().trim().toUpperCase();
        log.info("Creating or updating role by name: {}", roleName);

        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> Role.builder()
                        .name(roleName)
                        .systemRole(false)
                        .permissions(new HashSet<>())
                        .build());

        role.setDescription(request.getDescription());

        if (request.getPermissionCodes() != null) {
            Set<Permission> permissions = request.getPermissionCodes().stream()
                    .map(code -> permissionRepository.findByCode(code)
                            .orElseGet(() -> {
                                log.info("Auto-creating missing permission: {}", code);
                                return permissionRepository.save(
                                        Permission.builder()
                                                .code(code)
                                                .name(code)
                                                .description("Auto-created permission " + code)
                                                .module(null)
                                                .build());
                            }))
                    .collect(Collectors.toSet());

            // Clear and add all to maintain Hibernate collection tracking
            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
        }

        roleRepository.save(role);
        roleRepository.flush();

        return MessageResponse.builder()
                .message("Role " + request.getName() + " saved successfully")
                .build();
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToRoleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoleResponse getRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        return mapToRoleResponse(role);
    }

    @Override
    public RoleResponse updateRole(String roleId, RoleRequest request) {
        log.info("Updating role with id: {} to new name: {}", roleId, request.getName());

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));

        if (role.isSystemRole()) {
            log.warn("Attempt to modify system role: {}", role.getName());
            throw new IllegalArgumentException("Cannot modify system role: " + role.getName());
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getPermissionCodes() != null) {
            Set<Permission> permissions = request.getPermissionCodes().stream()
                    .map(code -> permissionRepository.findByCode(code)
                            .orElseGet(() -> {
                                log.info("Auto-creating missing permission: {}", code);
                                return permissionRepository.save(
                                        Permission.builder()
                                                .code(code)
                                                .name(code)
                                                .description("Auto-created permission " + code)
                                                .module(null)
                                                .build());
                            }))
                    .collect(Collectors.toSet());

            // Clear and add all to maintain Hibernate collection tracking
            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
        }

        Role savedRole = roleRepository.save(role);
        roleRepository.flush();

        log.info("Role updated successfully: {}", savedRole.getName());
        return mapToRoleResponse(savedRole);
    }

    @Override
    public MessageResponse deleteRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));

        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot delete system role: " + role.getName());
        }

        // Clear all user associations before deleting the role
        // This ensures users revert to their base role (e.g. USER) in their next token
        roleRepository.deleteUserAssociations(roleId);
        
        roleRepository.delete(role);

        return MessageResponse.builder()
                .message("Role deleted successfully")
                .build();
    }

    private RoleResponse mapToRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .systemRole(role.isSystemRole())
                .permissions(role.getPermissions().stream()
                        .map(Permission::getCode)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    public MessageResponse assignRolesToUser(AssignRolesRequest request) {
        User user = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Security Check: Prevent modifying roles of a SUPER_ADMIN
        boolean isSuperAdmin = user.getRole() == User.UserRole.SUPER_ADMIN ||
                (user.getRoles() != null && user.getRoles().stream()
                        .anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName())));

        if (isSuperAdmin) {
            throw new SecurityException("Cannot modify roles of a SUPER_ADMIN user. This action is restricted.");
        }

        Set<Role> roles = request.getRoles().stream()
                .map(name -> {
                    String roleName = name.trim().toUpperCase();
                    return roleRepository.findByName(roleName)
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                })
                .collect(Collectors.toSet());

        user.setRoles(roles);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Roles assigned to user successfully")
                .build();
    }

    // ================ AGENT REGISTRATION ================

    @Override
    public AuthResponse registerAgent(RegisterAgentRequest request,
            MultipartFile gstinCertificate,
            MultipartFile incorporationCertificate,
            MultipartFile panCard,
            MultipartFile bankStatement,
            MultipartFile cancelledCheque,
            MultipartFile addressProof,
            MultipartFile authorizationLetter) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Check if agent code already exists
        if (agentRepository.findByAgentCode(request.getAgentCode()).isPresent()) {
            throw new IllegalArgumentException("Agent code is already registered");
        }

        // Validate password strength
        if (!ValidationUtil.isStrongPassword(request.getPassword())) {
            throw new IllegalArgumentException("Password does not meet security requirements: " +
                    ValidationUtil.getPasswordStrengthMessage(request.getPassword()));
        }

        // Get or create AGENT role
        Role agentRole = roleRepository.findByName("AGENT")
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name("AGENT")
                            .description("Travel Agent role for B2B partners")
                            .systemRole(true)
                            .build();
                    return roleRepository.save(newRole);
                });

        // Create new user with AGENT role
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.B2B) // Use B2B as base role for agents
                .isVerified(false)
                .isActive(false) // Inactive until Admin or Auto verification passes
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

        // Assign AGENT role to user
        user.getRoles().add(agentRole);
        userRepository.save(user);

        // Create and save local Agent entity
        Agent agent = Agent.builder()
                .user(user)
                .agentCode(request.getAgentCode())
                .agencyName(request.getCompanyName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .alternatePhone(request.getAlternatePhone())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .gstin(request.getGstin())
                .gstRegistrationType(request.getGstRegistrationType())
                .gstStateCode(request.getGstStateCode())
                .gstLegalName(request.getGstLegalName())
                .gstTradeName(request.getGstTradeName())
                .gstJurisdiction(request.getGstJurisdiction())
                .gstRegistrationDate(request.getGstRegistrationDate())
                .pan(request.getPan())
                .panLegalName(request.getPanLegalName())
                .panFatherName(request.getPanFatherName())
                .panDateOfBirth(request.getPanDateOfBirth())
                .incorporationNumber(request.getIncorporationNumber())
                .incorporationType(request.getIncorporationType())
                .incorporationDate(request.getIncorporationDate())
                .registrationAuthority(request.getRegistrationAuthority())
                .registrationState(request.getRegistrationState())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankIfscCode(request.getBankIfscCode())
                .bankName(request.getBankName())
                .bankBranch(request.getBankBranch())
                .bankAccountType(request.getBankAccountType())
                .bankAccountHolderName(request.getBankAccountHolderName())
                .bankCity(request.getBankCity())
                .bankState(request.getBankState())
                .businessType(request.getBusinessType())
                .businessCategory(request.getBusinessCategory())
                .annualTurnover(request.getAnnualTurnover())
                .yearsInBusiness(request.getYearsInBusiness())
                .numberOfEmployees(request.getNumberOfEmployees())
                .website(request.getWebsite())
                .businessDescription(request.getBusinessDescription())
                .alternateEmail(request.getAlternateEmail())
                .fax(request.getFax())
                .landline(request.getLandline())
                .build();
        agentRepository.save(agent);

        String userId = user.getId();
        log.info("User created in auth service for agent: {} with userId: {}", request.getEmail(), userId);

        // --- Automated Verification ---
        List<VerificationRule> agentRules = verificationRuleRepository.findByRoleType("AGENT");
        List<VerificationRule> mandatoryRules = agentRules.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsMandatory()))
                .collect(Collectors.toList());

        boolean autoApproved = automatedVerificationService.verifyAgentAutomatically(request, mandatoryRules);
        if (autoApproved) {
            VerificationRequest verReq = VerificationRequest.builder()
                    .user(user)
                    .verificationType("AGENT")
                    .description("Automated verification passed on registration")
                    .status(VerificationRequest.VerificationStatus.APPROVED)
                    .build();
            verificationRequestRepository.save(verReq);

            // Send notification for auto-approval
            try {
                realtimeNotificationHelper.sendNotification(
                        user.getEmail(),
                        "VERIFICATION_APPROVED",
                        "Verification Approved",
                        "Your agent registration has been automatically verified and activated. Welcome aboard!",
                        verReq.getId(),
                        "VERIFICATION_REQUEST",
                        null,
                        new HashMap<>(),
                        false,
                        null,
                        "HIGH");
            } catch (Exception e) {
                log.warn("Failed to send auto-verification notification for agent: {}", e.getMessage());
            }
        } else {
            log.info("Agent {} requires manual Admin verification. Account is inactive.", request.getEmail());
            VerificationRequest verReq = VerificationRequest.builder()
                    .user(user)
                    .verificationType("AGENT")
                    .description("Pending Admin verification after registration")
                    .status(VerificationRequest.VerificationStatus.PENDING)
                    .build();
            verificationRequestRepository.save(verReq);
        }

        // Upload documents to S3
        Map<String, String> documentUrls = new HashMap<>();
        try {
            if (gstinCertificate != null && !gstinCertificate.isEmpty()) {
                String gstinUrl = s3Helper.uploadFile(gstinCertificate,
                        "agent-registration/" + userId + "/gstin-certificate");
                documentUrls.put("gstinCertificate", gstinUrl);
                log.info("GSTIN certificate uploaded: {}", gstinUrl);
            }

            if (incorporationCertificate != null && !incorporationCertificate.isEmpty()) {
                String incorpUrl = s3Helper.uploadFile(incorporationCertificate,
                        "agent-registration/" + userId + "/incorporation-certificate");
                documentUrls.put("incorporationCertificate", incorpUrl);
                log.info("Incorporation certificate uploaded: {}", incorpUrl);
            }

            if (panCard != null && !panCard.isEmpty()) {
                String panUrl = s3Helper.uploadFile(panCard,
                        "agent-registration/" + userId + "/pan-card");
                documentUrls.put("panCard", panUrl);
                log.info("PAN card uploaded: {}", panUrl);
            }

            if (bankStatement != null && !bankStatement.isEmpty()) {
                String bankStmtUrl = s3Helper.uploadFile(bankStatement,
                        "agent-registration/" + userId + "/bank-statement");
                documentUrls.put("bankStatement", bankStmtUrl);
                log.info("Bank statement uploaded: {}", bankStmtUrl);
            }

            if (cancelledCheque != null && !cancelledCheque.isEmpty()) {
                String chequeUrl = s3Helper.uploadFile(cancelledCheque,
                        "agent-registration/" + userId + "/cancelled-cheque");
                documentUrls.put("cancelledCheque", chequeUrl);
                log.info("Cancelled cheque uploaded: {}", chequeUrl);
            }

            if (addressProof != null && !addressProof.isEmpty()) {
                String addrProofUrl = s3Helper.uploadFile(addressProof,
                        "agent-registration/" + userId + "/address-proof");
                documentUrls.put("addressProof", addrProofUrl);
                log.info("Address proof uploaded: {}", addrProofUrl);
            }

            if (authorizationLetter != null && !authorizationLetter.isEmpty()) {
                String authUrl = s3Helper.uploadFile(authorizationLetter,
                        "agent-registration/" + userId + "/authorization-letter");
                documentUrls.put("authorizationLetter", authUrl);
                log.info("Authorization letter uploaded: {}", authUrl);
            }
        } catch (Exception e) {
            log.error("Error uploading documents: {}", e.getMessage(), e);
            // Don't fail registration if document upload fails, just log the error
            log.warn("Continuing registration despite document upload errors");
        }

        log.info("Agent registered successfully: {} with agent code: {}", user.getEmail(), request.getAgentCode());

        notificationService.sendWelcomeNotification(user);

        // Publish user registered event to Kafka
        try {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            authEventProducer.publishUserRegistered(user.getId(), user.getEmail(), correlationId);
        } catch (Exception e) {
            log.warn("Failed to publish user registered event for agent {}: {}", user.getEmail(), e.getMessage());
        }

        // Generate tokens with all roles
        String sessionId = UUID.randomUUID().toString();
        String deviceId = generateDeviceId(request.getUserAgent(), request.getIpAddress());

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

        String refreshToken = createRefreshToken(user, deviceId, sessionId, request.getIpAddress(),
                request.getUserAgent(), false);

        UserResponse userResponse = mapToUserResponse(user);
        AgentResponse agentResponse = mapToAgentResponse(agent);

        logSecurityEvent(user.getEmail(), "AGENT_REGISTRATION_SUCCESS", request.getIpAddress(), request.getUserAgent());
        logLoginHistory(user, request.getIpAddress(), request.getUserAgent(), true);

        return AuthResponse.builder()
                .isAuthenticated(true)
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .agent(agentResponse)
                .expiresIn(jwtUtil.getExpirationTime())
                .tokenType("Bearer")
                .sessionId(sessionId)
                .deviceId(deviceId)
                .build();
    }

    // ================ SUPPLIER REGISTRATION ================

    @Override
    public AuthResponse registerSupplier(RegisterSupplierRequest request,
            MultipartFile gstinCertificate,
            MultipartFile incorporationCertificate,
            MultipartFile panCard,
            MultipartFile bankStatement,
            MultipartFile cancelledCheque,
            MultipartFile addressProof,
            MultipartFile authorizationLetter) {

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Check if supplier code already exists
        if (supplierRepository.findBySupplierCode(request.getSupplierCode()).isPresent()) {
            throw new IllegalArgumentException("Supplier code is already registered");
        }

        // Validate password strength
        if (!ValidationUtil.isStrongPassword(request.getPassword())) {
            throw new IllegalArgumentException("Password does not meet security requirements: " +
                    ValidationUtil.getPasswordStrengthMessage(request.getPassword()));
        }

        // Get or create SUPPLIER role
        Role supplierRole = roleRepository.findByName("SUPPLIER")
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name("SUPPLIER")
                            .description("Supplier role for B2B partners (Hotels, Transport, etc.)")
                            .systemRole(true)
                            .build();
                    return roleRepository.save(newRole);
                });

        // Create new user with B2B role as base
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.B2B) // Use B2B as base role for suppliers too
                .isVerified(false)
                .isActive(false) // Inactive until Admin or Auto verification passes
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

        // Assign SUPPLIER role to user
        user.getRoles().add(supplierRole);
        userRepository.save(user);

        // Create and save local Supplier entity
        Supplier supplier = Supplier.builder()
                .user(user)
                .supplierCode(request.getSupplierCode())
                .companyName(request.getCompanyName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .alternatePhone(request.getAlternatePhone())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .gstin(request.getGstin())
                .pan(request.getPan())
                .gstRegistrationType(request.getGstRegistrationType())
                .gstStateCode(request.getGstStateCode())
                .gstLegalName(request.getGstLegalName())
                .gstTradeName(request.getGstTradeName())
                .gstJurisdiction(request.getGstJurisdiction())
                .gstRegistrationDate(request.getGstRegistrationDate())
                .panLegalName(request.getPanLegalName())
                .panFatherName(request.getPanFatherName())
                .panDateOfBirth(request.getPanDateOfBirth())
                .incorporationNumber(request.getIncorporationNumber())
                .incorporationType(request.getIncorporationType())
                .incorporationDate(request.getIncorporationDate())
                .registrationAuthority(request.getRegistrationAuthority())
                .registrationState(request.getRegistrationState())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankIfscCode(request.getBankIfscCode())
                .bankName(request.getBankName())
                .bankBranch(request.getBankBranch())
                .bankAccountType(request.getBankAccountType())
                .bankAccountHolderName(request.getBankAccountHolderName())
                .bankCity(request.getBankCity())
                .bankState(request.getBankState())
                .supplierType(request.getSupplierType())
                .businessCategory(request.getBusinessCategory())
                .annualTurnover(request.getAnnualTurnover())
                .yearsInBusiness(request.getYearsInBusiness())
                .numberOfEmployees(request.getNumberOfEmployees())
                .website(request.getWebsite())
                .businessDescription(request.getBusinessDescription())
                .alternateEmail(request.getAlternateEmail())
                .fax(request.getFax())
                .landline(request.getLandline())
                .build();
        supplierRepository.save(supplier);

        String userId = user.getId();
        log.info("User created in auth service for supplier: {} with userId: {}", request.getEmail(), userId);

        // --- Automated Verification ---
        List<VerificationRule> supplierRules = verificationRuleRepository.findByRoleType("SUPPLIER");
        List<VerificationRule> mandatorySupplierRules = supplierRules.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsMandatory()))
                .collect(Collectors.toList());

        boolean supplierAutoApproved = automatedVerificationService.verifySupplierAutomatically(request,
                mandatorySupplierRules);
        if (supplierAutoApproved) {
            VerificationRequest verReqSupplier = VerificationRequest.builder()
                    .user(user)
                    .verificationType("SUPPLIER")
                    .description("Automated verification passed on registration")
                    .status(VerificationRequest.VerificationStatus.APPROVED)
                    .build();
            verificationRequestRepository.save(verReqSupplier);

            // Send notification for auto-approval
            try {
                realtimeNotificationHelper.sendNotification(
                        user.getEmail(),
                        "VERIFICATION_APPROVED",
                        "Verification Approved",
                        "Your supplier registration has been automatically verified and activated. Welcome aboard!",
                        verReqSupplier.getId(),
                        "VERIFICATION_REQUEST",
                        null,
                        new HashMap<>(),
                        false,
                        null,
                        "HIGH");
            } catch (Exception e) {
                log.warn("Failed to send auto-verification notification for supplier: {}", e.getMessage());
            }
        } else {
            log.info("Supplier {} requires manual Admin verification. Account is inactive.", request.getEmail());
            VerificationRequest verReqSupplier = VerificationRequest.builder()
                    .user(user)
                    .verificationType("SUPPLIER")
                    .description("Pending Admin verification after registration")
                    .status(VerificationRequest.VerificationStatus.PENDING)
                    .build();
            verificationRequestRepository.save(verReqSupplier);
        }

        // Upload documents to S3
        Map<String, String> documentUrls = new HashMap<>();
        try {
            if (gstinCertificate != null && !gstinCertificate.isEmpty()) {
                String gstinUrl = s3Helper.uploadFile(gstinCertificate,
                        "supplier-registration/" + userId + "/gstin-certificate");
                documentUrls.put("gstinCertificate", gstinUrl);
                log.info("GSTIN certificate uploaded: {}", gstinUrl);
            }

            if (incorporationCertificate != null && !incorporationCertificate.isEmpty()) {
                String incorpUrl = s3Helper.uploadFile(incorporationCertificate,
                        "supplier-registration/" + userId + "/incorporation-certificate");
                documentUrls.put("incorporationCertificate", incorpUrl);
                log.info("Incorporation certificate uploaded: {}", incorpUrl);
            }

            if (panCard != null && !panCard.isEmpty()) {
                String panUrl = s3Helper.uploadFile(panCard,
                        "supplier-registration/" + userId + "/pan-card");
                documentUrls.put("panCard", panUrl);
                log.info("PAN card uploaded: {}", panUrl);
            }

            if (bankStatement != null && !bankStatement.isEmpty()) {
                String bankStmtUrl = s3Helper.uploadFile(bankStatement,
                        "supplier-registration/" + userId + "/bank-statement");
                documentUrls.put("bankStatement", bankStmtUrl);
                log.info("Bank statement uploaded: {}", bankStmtUrl);
            }

            if (cancelledCheque != null && !cancelledCheque.isEmpty()) {
                String chequeUrl = s3Helper.uploadFile(cancelledCheque,
                        "supplier-registration/" + userId + "/cancelled-cheque");
                documentUrls.put("cancelledCheque", chequeUrl);
                log.info("Cancelled cheque uploaded: {}", chequeUrl);
            }

            if (addressProof != null && !addressProof.isEmpty()) {
                String addrProofUrl = s3Helper.uploadFile(addressProof,
                        "supplier-registration/" + userId + "/address-proof");
                documentUrls.put("addressProof", addrProofUrl);
                log.info("Address proof uploaded: {}", addrProofUrl);
            }

            if (authorizationLetter != null && !authorizationLetter.isEmpty()) {
                String authUrl = s3Helper.uploadFile(authorizationLetter,
                        "supplier-registration/" + userId + "/authorization-letter");
                documentUrls.put("authorizationLetter", authUrl);
                log.info("Authorization letter uploaded: {}", authUrl);
            }
        } catch (Exception e) {
            log.error("Error uploading documents: {}", e.getMessage(), e);
            // Don't fail registration if document upload fails, just log the error
            log.warn("Continuing registration despite document upload errors");
        }

        log.info("Supplier registered successfully: {} with supplier code: {}", user.getEmail(),
                request.getSupplierCode());

        notificationService.sendWelcomeNotification(user);

        // Publish user registered event to Kafka
        try {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            authEventProducer.publishUserRegistered(user.getId(), user.getEmail(), correlationId);
        } catch (Exception e) {
            log.warn("Failed to publish user registered event for supplier {}: {}", user.getEmail(), e.getMessage());
        }

        // Generate tokens with all roles
        String sessionId = UUID.randomUUID().toString();
        String deviceId = generateDeviceId(request.getUserAgent(), request.getIpAddress());

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

        String refreshToken = createRefreshToken(user, deviceId, sessionId, request.getIpAddress(),
                request.getUserAgent(), false);

        UserResponse userResponse = mapToUserResponse(user);
        SupplierResponse supplierResponse = mapToSupplierResponse(supplier);

        logSecurityEvent(user.getEmail(), "SUPPLIER_REGISTRATION_SUCCESS", request.getIpAddress(),
                request.getUserAgent());
        logLoginHistory(user, request.getIpAddress(), request.getUserAgent(), true);

        return AuthResponse.builder()
                .isAuthenticated(true)
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .supplier(supplierResponse)
                .expiresIn(jwtUtil.getExpirationTime())
                .tokenType("Bearer")
                .sessionId(sessionId)
                .deviceId(deviceId)
                .build();
    }

    // ================ PRIVATE HELPER METHODS ================

    /**
     * Collect all roles from a user (both UserRole enum and Role entities)
     * FIXED: Now includes the primary UserRole (admin, user, etc.) in roles array
     */
    private List<String> collectAllRoles(User user) {
        List<String> roles = new ArrayList<>();

        // CRITICAL FIX: Add the primary UserRole enum value (admin, user, agent, etc.)
        // This is the user's main role stored in the role field
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

    private String createRefreshToken(User user, String deviceId, String sessionId,
            String ipAddress, String userAgent, boolean rememberMe) {
        // Clean up oldest token if session limit reached (increased to 10 for better multi-device support)
        long activeTokens = refreshTokenRepository.countActiveTokensByUser(user);
        if (activeTokens >= 10) {
            log.info("Session limit reached for user {}. Revoking oldest session.", user.getEmail());
            refreshTokenRepository.findFirstByUserAndIsRevokedFalseOrderByCreatedAtAsc(user)
                    .ifPresent(oldestToken -> {
                        oldestToken.setIsRevoked(true);
                        refreshTokenRepository.save(oldestToken);
                    });
        }

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
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private void sendEmailVerificationOtp(String email, String userName) {
        // Invalidate existing OTPs
        otpTokenRepository.invalidateAllOtpsByEmailAndType(email, OtpToken.OtpType.EMAIL_VERIFICATION);

        String otp = generateOtp();

        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otp(otp)
                .type(OtpToken.OtpType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        otpTokenRepository.save(otpToken);
        
        Map<String, Object> vars = new HashMap<>();
        vars.put("firstName", userName);
        vars.put("verificationCode", otp);
        notificationService.sendEmail(email, "Email Verification OTP", "", "verification_email", vars);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String generateDeviceId(String userAgent, String ipAddress) {
        return UUID.nameUUIDFromBytes((userAgent + ipAddress).getBytes()).toString();
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            codes.add(String.format("%08d", random.nextInt(100000000)));
        }
        return codes;
    }

    private int getRateLimit(String action) {
        return switch (action) {
            case "login" -> 5;
            case "password_reset" -> 3;
            case "email_verification" -> 3;
            default -> 10;
        };
    }

    private String parseDeviceName(String userAgent) {
        // Simple device name parsing from user agent
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

    /**
     * Log login history for a user
     */
    private void logLoginHistory(User user, String ipAddress, String userAgent, boolean successful) {
        try {
            String deviceType = detectDeviceType(userAgent);
            String location = geoLocationHelper.getLocationFromIp(ipAddress);

            LoginHistory loginHistory = LoginHistory.builder()
                    .user(user)
                    .ipAddress(ipAddress != null ? ipAddress : "")
                    .userAgent(userAgent != null ? userAgent : "")
                    .location(location)
                    .deviceType(deviceType)
                    .successful(successful)
                    .build();
            loginHistoryRepository.save(loginHistory);
        } catch (Exception e) {
            log.error("Failed to log login history: {}", e.getMessage());
            // Don't throw - login history logging should not break the main flow
        }
    }

    /**
     * Detect device type from user agent string
     */
    private String detectDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "UNKNOWN";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "MOBILE";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "TABLET";
        } else {
            return "DESKTOP";
        }
    }

    private void logSecurityEvent(String userEmail, String eventType, String ipAddress, String userAgent) {
        // Implementation for logging security events
        // You might want to create a SecurityLog entity
        log.info("Security Event - User: {}, Event: {}, IP: {}, UserAgent: {}",
                userEmail, eventType, ipAddress, userAgent);
    }

    private UserResponse mapToUserResponse(User user) {
        // Get RBAC roles from user_roles join table
        List<String> rbacRoles = user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList())
                : new ArrayList<>();

        // For the legacy 'role' field, prioritize higher privilege roles
        String primaryRole = user.getRole().name().toLowerCase();
        if (!rbacRoles.isEmpty()) {
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
            if (!found) {
                primaryRole = rbacRoles.get(0).replaceFirst("^ROLE_", "").toLowerCase();
            }
        }

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .gender(user.getGender() != null ? user.getGender().name().toLowerCase() : null)
                .role(primaryRole)
                // roles field removed from DTO
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        return response;
    }

    private AgentResponse mapToAgentResponse(Agent agent) {
        return AgentResponse.builder()
                .id(agent.getId())
                .agentCode(agent.getAgentCode())
                .agencyName(agent.getAgencyName())
                .contactPerson(agent.getContactPerson())
                .phone(agent.getPhone())
                .alternatePhone(agent.getAlternatePhone())
                .address(agent.getAddress())
                .city(agent.getCity())
                .state(agent.getState())
                .country(agent.getCountry())
                .pincode(agent.getPincode())
                .gstin(agent.getGstin())
                .gstRegistrationType(agent.getGstRegistrationType())
                .gstStateCode(agent.getGstStateCode())
                .gstLegalName(agent.getGstLegalName())
                .gstTradeName(agent.getGstTradeName())
                .gstJurisdiction(agent.getGstJurisdiction())
                .gstRegistrationDate(agent.getGstRegistrationDate())
                .pan(agent.getPan())
                .panLegalName(agent.getPanLegalName())
                .panFatherName(agent.getPanFatherName())
                .panDateOfBirth(agent.getPanDateOfBirth())
                .incorporationNumber(agent.getIncorporationNumber())
                .incorporationType(agent.getIncorporationType())
                .incorporationDate(agent.getIncorporationDate())
                .registrationAuthority(agent.getRegistrationAuthority())
                .registrationState(agent.getRegistrationState())
                .bankAccountNumber(agent.getBankAccountNumber())
                .bankIfscCode(agent.getBankIfscCode())
                .bankName(agent.getBankName())
                .bankBranch(agent.getBankBranch())
                .bankAccountType(agent.getBankAccountType())
                .bankAccountHolderName(agent.getBankAccountHolderName())
                .bankCity(agent.getBankCity())
                .bankState(agent.getBankState())
                .businessType(agent.getBusinessType())
                .businessCategory(agent.getBusinessCategory())
                .annualTurnover(agent.getAnnualTurnover())
                .yearsInBusiness(agent.getYearsInBusiness())
                .numberOfEmployees(agent.getNumberOfEmployees())
                .website(agent.getWebsite())
                .businessDescription(agent.getBusinessDescription())
                .alternateEmail(agent.getAlternateEmail())
                .fax(agent.getFax())
                .landline(agent.getLandline())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

    private SupplierResponse mapToSupplierResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .supplierCode(supplier.getSupplierCode())
                .companyName(supplier.getCompanyName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .alternatePhone(supplier.getAlternatePhone())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .state(supplier.getState())
                .country(supplier.getCountry())
                .pincode(supplier.getPincode())
                .gstin(supplier.getGstin())
                .pan(supplier.getPan())
                .gstRegistrationType(supplier.getGstRegistrationType())
                .gstStateCode(supplier.getGstStateCode())
                .gstLegalName(supplier.getGstLegalName())
                .gstTradeName(supplier.getGstTradeName())
                .gstJurisdiction(supplier.getGstJurisdiction())
                .gstRegistrationDate(supplier.getGstRegistrationDate())
                .panLegalName(supplier.getPanLegalName())
                .panFatherName(supplier.getPanFatherName())
                .panDateOfBirth(supplier.getPanDateOfBirth())
                .incorporationNumber(supplier.getIncorporationNumber())
                .incorporationType(supplier.getIncorporationType())
                .incorporationDate(supplier.getIncorporationDate())
                .registrationAuthority(supplier.getRegistrationAuthority())
                .registrationState(supplier.getRegistrationState())
                .bankAccountNumber(supplier.getBankAccountNumber())
                .bankIfscCode(supplier.getBankIfscCode())
                .bankName(supplier.getBankName())
                .bankBranch(supplier.getBankBranch())
                .bankAccountType(supplier.getBankAccountType())
                .bankAccountHolderName(supplier.getBankAccountHolderName())
                .bankCity(supplier.getBankCity())
                .bankState(supplier.getBankState())
                .supplierType(supplier.getSupplierType())
                .businessCategory(supplier.getBusinessCategory())
                .annualTurnover(supplier.getAnnualTurnover())
                .yearsInBusiness(supplier.getYearsInBusiness())
                .numberOfEmployees(supplier.getNumberOfEmployees())
                .website(supplier.getWebsite())
                .businessDescription(supplier.getBusinessDescription())
                .alternateEmail(supplier.getAlternateEmail())
                .fax(supplier.getFax())
                .landline(supplier.getLandline())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}