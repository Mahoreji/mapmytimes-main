package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.auth.*;
import in.mapmytour.auth.dto.user.AdminLoginHistoryItemResponse;
import in.mapmytour.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuthService {

    // Basic Authentication
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    MessageResponse sendOtp(String request);

    AuthResponse loginWithOtp(LoginWithOtpRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse registerAgent(RegisterAgentRequest request,
            org.springframework.web.multipart.MultipartFile gstinCertificate,
            org.springframework.web.multipart.MultipartFile incorporationCertificate,
            org.springframework.web.multipart.MultipartFile panCard,
            org.springframework.web.multipart.MultipartFile bankStatement,
            org.springframework.web.multipart.MultipartFile cancelledCheque,
            org.springframework.web.multipart.MultipartFile addressProof,
            org.springframework.web.multipart.MultipartFile authorizationLetter);

    AuthResponse registerSupplier(RegisterSupplierRequest request,
            org.springframework.web.multipart.MultipartFile gstinCertificate,
            org.springframework.web.multipart.MultipartFile incorporationCertificate,
            org.springframework.web.multipart.MultipartFile panCard,
            org.springframework.web.multipart.MultipartFile bankStatement,
            org.springframework.web.multipart.MultipartFile cancelledCheque,
            org.springframework.web.multipart.MultipartFile addressProof,
            org.springframework.web.multipart.MultipartFile authorizationLetter);

    MessageResponse logout(String userEmail, String refreshToken);

    MessageResponse logoutAllDevices(String userEmail);

    // Password Management
    MessageResponse forgotPasswordStep1(ForgotPasswordStep1Request request);

    TokenResponse forgotPasswordStep2(ForgotPasswordStep2Request request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    MessageResponse changePassword(ChangePasswordRequest request, String userEmail);

    MessageResponse validatePassword(String password);

    // Email Verification
    AuthResponse verifyEmail(VerifyEmailRequest request);

    MessageResponse resendVerificationEmail(ResendVerificationRequest request);

    MessageResponse sendVerificationOtp(String email);

    // Token Management
    AuthResponse refreshToken(RefreshTokenRequest request);

    MessageResponse revokeToken(RevokeTokenRequest request);

    MessageResponse revokeAllTokens(String userEmail);

    TokenValidationResponse validateToken(String token);

    // Account Management
    UserResponse getCurrentUser(String userEmail);

    MessageResponse deactivateAccount(String userEmail, String password);

    MessageResponse reactivateAccount(ReactivateAccountRequest request);

    MessageResponse requestAccountDeletion(String userEmail);

    MessageResponse confirmAccountDeletion(ConfirmAccountDeletionRequest request);

    // Security Features
    MessageResponse enableTwoFactorAuth(String userEmail);

    MessageResponse disableTwoFactorAuth(String userEmail, String code);

    MessageResponse generateBackupCodes(String userEmail);

    TwoFactorResponse verifyTwoFactorCode(TwoFactorVerificationRequest request);

    // Account Status
    EmailCheckResponse checkEmailExists(String email);

    AccountStatusResponse getAccountStatus(String email);

    MessageResponse unlockAccount(UnlockAccountRequest request);

    // Session Management
    List<UserSessionResponse> getActiveSessions(String userEmail);

    MessageResponse terminateSession(String userEmail, String sessionId);

    MessageResponse terminateAllOtherSessions(String userEmail, String currentSessionId);

    // Admin Functions
    Page<UserResponse> getAllUsers(Pageable pageable, String search, String role, Boolean isActive);

    Page<AdminLoginHistoryItemResponse> getAllLoginHistory(Pageable pageable, String userEmail);

    MessageResponse adminResetPassword(AdminResetPasswordRequest request);

    MessageResponse adminUnlockAccount(String userEmail);

    MessageResponse adminDeactivateUser(String userEmail, String reason);

    MessageResponse adminActivateUser(String userEmail);

    UserStatsResponse getUserStats(String userEmail);

    // Security Logs
    Page<SecurityLogResponse> getSecurityLogs(String userEmail, Pageable pageable);

    MessageResponse reportSuspiciousActivity(SecurityReportRequest request);

    // Device Management
    List<DeviceResponse> getRegisteredDevices(String userEmail);

    MessageResponse registerDevice(String userEmail, DeviceRegistrationRequest request);

    MessageResponse revokeDevice(String userEmail, String deviceId);

    // Rate Limiting
    boolean isRateLimited(String identifier, String action);

    MessageResponse checkRateLimit(String identifier, String action);

    // RBAC Management (SUPER_ADMIN only via controller)
    MessageResponse createOrUpdateRole(RoleRequest request);

    List<RoleResponse> getAllRoles();

    RoleResponse getRole(String roleId);

    RoleResponse updateRole(String roleId, RoleRequest request);

    MessageResponse deleteRole(String roleId);

    MessageResponse assignRolesToUser(AssignRolesRequest request);
}