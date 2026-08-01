package in.mapmytour.auth.controller;

import in.mapmytour.auth.dto.APIResponse;
import in.mapmytour.auth.dto.auth.*;
import in.mapmytour.auth.dto.user.AdminLoginHistoryItemResponse;
import in.mapmytour.auth.service.AuthService;
import in.mapmytour.auth.service.UserContextService;
import in.mapmytour.auth.utils.APIResponseUtil;
import in.mapmytour.auth.utils.ValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserContextService userContextService;

    // ================ BASIC AUTHENTICATION ================

    /**
     * Register new user
     */
    @PostMapping("/register")
    public ResponseEntity<APIResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        log.info("Registration attempt for email: {}", request.getEmail());

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            AuthResponse response = authService.register(request);
            log.info("User registered successfully: {}", request.getEmail());
            return APIResponseUtil.created(response, "User registered successfully. Please verify your email.");
        } catch (Exception e) {
            log.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Register new agent (creates user with AGENT role) - Multipart/form-data
     * support
     */
    @PostMapping(value = "/register/agent", consumes = "multipart/form-data")
    public ResponseEntity<APIResponse<AuthResponse>> registerAgent(
            @RequestPart("firstName") String firstName,
            @RequestPart("lastName") String lastName,
            @RequestPart("email") String email,
            @RequestPart("password") String password,
            @RequestPart("confirmPassword") String confirmPassword,
            @RequestPart("phone") String phone,
            @RequestPart("agentCode") String agentCode,
            @RequestPart("companyName") String companyName,
            @RequestPart("contactPerson") String contactPerson,
            @RequestPart("address") String address,
            @RequestPart("city") String city,
            @RequestPart("state") String state,
            @RequestPart("country") String country,
            @RequestPart("pincode") String pincode,
            @RequestPart("bankAccountNumber") String bankAccountNumber,
            @RequestPart("bankIfscCode") String bankIfscCode,
            @RequestPart("bankName") String bankName,
            @RequestPart("bankBranch") String bankBranch,
            @RequestPart("bankAccountType") String bankAccountType,
            @RequestPart(value = "bankAccountHolderName", required = false) String bankAccountHolderName,
            @RequestPart(value = "agreeToTerms") String agreeToTerms,
            @RequestPart(value = "alternatePhone", required = false) String alternatePhone,
            @RequestPart(value = "gstin", required = false) String gstin,
            @RequestPart(value = "pan", required = false) String pan,
            @RequestPart(value = "gstRegistrationType", required = false) String gstRegistrationType,
            @RequestPart(value = "gstStateCode", required = false) String gstStateCode,
            @RequestPart(value = "gstLegalName", required = false) String gstLegalName,
            @RequestPart(value = "gstTradeName", required = false) String gstTradeName,
            @RequestPart(value = "gstJurisdiction", required = false) String gstJurisdiction,
            @RequestPart(value = "gstRegistrationDate", required = false) String gstRegistrationDate,
            @RequestPart(value = "panLegalName", required = false) String panLegalName,
            @RequestPart(value = "panFatherName", required = false) String panFatherName,
            @RequestPart(value = "panDateOfBirth", required = false) String panDateOfBirth,
            @RequestPart(value = "incorporationNumber", required = false) String incorporationNumber,
            @RequestPart(value = "incorporationType", required = false) String incorporationType,
            @RequestPart(value = "incorporationDate", required = false) String incorporationDate,
            @RequestPart(value = "registrationAuthority", required = false) String registrationAuthority,
            @RequestPart(value = "registrationState", required = false) String registrationState,
            @RequestPart(value = "bankCity", required = false) String bankCity,
            @RequestPart(value = "bankState", required = false) String bankState,
            @RequestPart(value = "businessType", required = false) String businessType,
            @RequestPart(value = "businessCategory", required = false) String businessCategory,
            @RequestPart(value = "annualTurnover", required = false) String annualTurnover,
            @RequestPart(value = "yearsInBusiness", required = false) String yearsInBusiness,
            @RequestPart(value = "numberOfEmployees", required = false) String numberOfEmployees,
            @RequestPart(value = "website", required = false) String website,
            @RequestPart(value = "businessDescription", required = false) String businessDescription,
            @RequestPart(value = "alternateEmail", required = false) String alternateEmail,
            @RequestPart(value = "fax", required = false) String fax,
            @RequestPart(value = "landline", required = false) String landline,
            @RequestPart(value = "gstinCertificate", required = false) MultipartFile gstinCertificate,
            @RequestPart(value = "incorporationCertificate", required = false) MultipartFile incorporationCertificate,
            @RequestPart(value = "panCard", required = false) MultipartFile panCard,
            @RequestPart(value = "bankStatement", required = false) MultipartFile bankStatement,
            @RequestPart(value = "cancelledCheque", required = false) MultipartFile cancelledCheque,
            @RequestPart(value = "addressProof", required = false) MultipartFile addressProof,
            @RequestPart(value = "authorizationLetter", required = false) MultipartFile authorizationLetter,
            HttpServletRequest httpRequest) {

        log.info("Agent registration attempt for email: {} with agent code: {}", email, agentCode);

        try {
            // Extract IP address and user agent from request
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            // Build RegisterAgentRequest from multipart data
            RegisterAgentRequest request = RegisterAgentRequest.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(password)
                    .confirmPassword(confirmPassword)
                    .phone(phone)
                    .agreeToTerms(Boolean.parseBoolean(agreeToTerms))
                    .agentCode(agentCode)
                    .companyName(companyName)
                    .contactPerson(contactPerson)
                    .alternatePhone(alternatePhone)
                    .address(address)
                    .city(city)
                    .state(state)
                    .country(country)
                    .pincode(pincode)
                    .gstin(gstin)
                    .gstRegistrationType(gstRegistrationType)
                    .gstStateCode(gstStateCode)
                    .gstLegalName(gstLegalName)
                    .gstTradeName(gstTradeName)
                    .gstJurisdiction(gstJurisdiction)
                    .gstRegistrationDate(gstRegistrationDate)
                    .pan(pan)
                    .panLegalName(panLegalName)
                    .panFatherName(panFatherName)
                    .panDateOfBirth(panDateOfBirth)
                    .incorporationNumber(incorporationNumber)
                    .incorporationType(incorporationType)
                    .incorporationDate(incorporationDate)
                    .registrationAuthority(registrationAuthority)
                    .registrationState(registrationState)
                    .bankAccountNumber(bankAccountNumber)
                    .bankIfscCode(bankIfscCode)
                    .bankName(bankName)
                    .bankBranch(bankBranch)
                    .bankAccountType(bankAccountType)
                    .bankAccountHolderName(bankAccountHolderName)
                    .bankCity(bankCity)
                    .bankState(bankState)
                    .businessType(businessType)
                    .businessCategory(businessCategory)
                    .annualTurnover(annualTurnover)
                    .yearsInBusiness(yearsInBusiness)
                    .numberOfEmployees(numberOfEmployees)
                    .website(website)
                    .businessDescription(businessDescription)
                    .alternateEmail(alternateEmail)
                    .fax(fax)
                    .landline(landline)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            AuthResponse response = authService.registerAgent(request,
                    gstinCertificate, incorporationCertificate, panCard,
                    bankStatement, cancelledCheque, addressProof, authorizationLetter);
            log.info("Agent registered successfully: {} with agent code: {}", email, agentCode);
            return APIResponseUtil.created(response, "Agent registered successfully. Please verify your email.");
        } catch (Exception e) {
            log.error("Agent registration failed for email {}: {}", email, e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Register a new user with SUPPLIER role
     */
    @PostMapping(value = "/register/supplier", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<AuthResponse>> registerSupplier(
            @RequestPart("firstName") String firstName,
            @RequestPart("lastName") String lastName,
            @RequestPart("email") String email,
            @RequestPart("password") String password,
            @RequestPart("confirmPassword") String confirmPassword,
            @RequestPart("phone") String phone,
            @RequestPart("supplierCode") String supplierCode,
            @RequestPart("companyName") String companyName,
            @RequestPart("contactPerson") String contactPerson,
            @RequestPart("address") String address,
            @RequestPart("city") String city,
            @RequestPart("state") String state,
            @RequestPart("country") String country,
            @RequestPart("pincode") String pincode,
            @RequestPart("bankAccountNumber") String bankAccountNumber,
            @RequestPart("bankIfscCode") String bankIfscCode,
            @RequestPart("bankName") String bankName,
            @RequestPart("bankBranch") String bankBranch,
            @RequestPart("supplierType") String supplierType,
            @RequestPart(value = "bankAccountType", required = false) String bankAccountType,
            @RequestPart(value = "bankAccountHolderName", required = false) String bankAccountHolderName,
            @RequestPart(value = "agreeToTerms") String agreeToTerms,
            @RequestPart(value = "alternatePhone", required = false) String alternatePhone,
            @RequestPart("gstin") String gstin,
            @RequestPart("pan") String pan,
            @RequestPart(value = "gstRegistrationType", required = false) String gstRegistrationType,
            @RequestPart(value = "gstStateCode", required = false) String gstStateCode,
            @RequestPart(value = "gstLegalName", required = false) String gstLegalName,
            @RequestPart(value = "gstTradeName", required = false) String gstTradeName,
            @RequestPart(value = "gstJurisdiction", required = false) String gstJurisdiction,
            @RequestPart(value = "gstRegistrationDate", required = false) String gstRegistrationDate,
            @RequestPart(value = "panLegalName", required = false) String panLegalName,
            @RequestPart(value = "panFatherName", required = false) String panFatherName,
            @RequestPart(value = "panDateOfBirth", required = false) String panDateOfBirth,
            @RequestPart(value = "incorporationNumber", required = false) String incorporationNumber,
            @RequestPart(value = "incorporationType", required = false) String incorporationType,
            @RequestPart(value = "incorporationDate", required = false) String incorporationDate,
            @RequestPart(value = "registrationAuthority", required = false) String registrationAuthority,
            @RequestPart(value = "registrationState", required = false) String registrationState,
            @RequestPart(value = "bankCity", required = false) String bankCity,
            @RequestPart(value = "bankState", required = false) String bankState,
            @RequestPart(value = "businessCategory", required = false) String businessCategory,
            @RequestPart(value = "annualTurnover", required = false) String annualTurnover,
            @RequestPart(value = "yearsInBusiness", required = false) String yearsInBusiness,
            @RequestPart(value = "numberOfEmployees", required = false) String numberOfEmployees,
            @RequestPart(value = "website", required = false) String website,
            @RequestPart(value = "businessDescription", required = false) String businessDescription,
            @RequestPart(value = "alternateEmail", required = false) String alternateEmail,
            @RequestPart(value = "fax", required = false) String fax,
            @RequestPart(value = "landline", required = false) String landline,
            @RequestPart(value = "gstinCertificate", required = false) MultipartFile gstinCertificate,
            @RequestPart(value = "incorporationCertificate", required = false) MultipartFile incorporationCertificate,
            @RequestPart(value = "panCard", required = false) MultipartFile panCard,
            @RequestPart(value = "bankStatement", required = false) MultipartFile bankStatement,
            @RequestPart(value = "cancelledCheque", required = false) MultipartFile cancelledCheque,
            @RequestPart(value = "addressProof", required = false) MultipartFile addressProof,
            @RequestPart(value = "authorizationLetter", required = false) MultipartFile authorizationLetter,
            HttpServletRequest httpRequest) {

        log.info("Supplier registration attempt for email: {} with supplier code: {}", email, supplierCode);

        try {
            // Extract IP address and user agent from request
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            // Build RegisterSupplierRequest from multipart data
            RegisterSupplierRequest request = RegisterSupplierRequest.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(password)
                    .confirmPassword(confirmPassword)
                    .phone(phone)
                    .agreeToTerms(Boolean.parseBoolean(agreeToTerms))
                    .supplierCode(supplierCode)
                    .companyName(companyName)
                    .contactPerson(contactPerson)
                    .alternatePhone(alternatePhone)
                    .address(address)
                    .city(city)
                    .state(state)
                    .country(country)
                    .pincode(pincode)
                    .gstin(gstin)
                    .gstRegistrationType(gstRegistrationType)
                    .gstStateCode(gstStateCode)
                    .gstLegalName(gstLegalName)
                    .gstTradeName(gstTradeName)
                    .gstJurisdiction(gstJurisdiction)
                    .gstRegistrationDate(gstRegistrationDate)
                    .pan(pan)
                    .panLegalName(panLegalName)
                    .panFatherName(panFatherName)
                    .panDateOfBirth(panDateOfBirth)
                    .incorporationNumber(incorporationNumber)
                    .incorporationType(incorporationType)
                    .incorporationDate(incorporationDate)
                    .registrationAuthority(registrationAuthority)
                    .registrationState(registrationState)
                    .bankAccountNumber(bankAccountNumber)
                    .bankIfscCode(bankIfscCode)
                    .bankName(bankName)
                    .bankBranch(bankBranch)
                    .bankAccountType(bankAccountType)
                    .bankAccountHolderName(bankAccountHolderName)
                    .bankCity(bankCity)
                    .bankState(bankState)
                    .supplierType(supplierType)
                    .businessCategory(businessCategory)
                    .annualTurnover(annualTurnover)
                    .yearsInBusiness(yearsInBusiness)
                    .numberOfEmployees(numberOfEmployees)
                    .website(website)
                    .businessDescription(businessDescription)
                    .alternateEmail(alternateEmail)
                    .fax(fax)
                    .landline(landline)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            AuthResponse response = authService.registerSupplier(request,
                    gstinCertificate, incorporationCertificate, panCard,
                    bankStatement, cancelledCheque, addressProof, authorizationLetter);
            log.info("Supplier registered successfully: {} with supplier code: {}", email, supplierCode);
            return APIResponseUtil.created(response, "Supplier registered successfully. Please verify your email.");
        } catch (Exception e) {
            log.error("Supplier registration failed for email {}: {}", email, e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * 
     * Login user with email and password
     */
    @PostMapping("/login")
    public ResponseEntity<APIResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("Login attempt for email: {}", request.getEmail());

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            AuthResponse response = authService.login(request, ipAddress, userAgent);
            log.info("User logged in successfully: {}", request.getEmail());
            return APIResponseUtil.success(response, "Login successful");
        } catch (Exception e) {
            log.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return APIResponseUtil.unauthorized(e.getMessage());
        }
    }

    /*
     * Login with OTP Send OTP
     */
    @PostMapping("/send-otp")
    public ResponseEntity<APIResponse<MessageResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {
        try {
            MessageResponse response = authService.sendOtp(request.getEmail());
            return APIResponseUtil.success(response, "OTP sent to your email");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Login with OTP Verification
     */
    @PostMapping("/login-otp")
    public ResponseEntity<APIResponse<AuthResponse>> loginWithOtp(
            @Valid @RequestBody LoginWithOtpRequest request) {

        log.info("OTP login attempt for email: {}", request.getEmail());

        try {
            AuthResponse response = authService.loginWithOtp(request);
            log.info("User logged in with OTP successfully: {}", request.getEmail());
            return APIResponseUtil.success(response, "Login with OTP successful");
        } catch (Exception e) {
            log.error("OTP login failed for email {}: {}", request.getEmail(), e.getMessage());
            return APIResponseUtil.unauthorized(e.getMessage());
        }
    }

    /**
     * Logout user
     */
    @PostMapping("/logout")
    public ResponseEntity<APIResponse<MessageResponse>> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            String refreshToken = request != null ? request.getRefreshToken() : null;
            MessageResponse response = authService.logout(userContext.getEmail(), refreshToken);
            log.info("User logged out successfully: {}", userContext.getEmail());
            return APIResponseUtil.success(response, "Logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed for user {}: {}", userContext.getEmail(), e.getMessage());
            return APIResponseUtil.internalServerError("Logout failed");
        }
    }

    /**
     * Logout from all devices
     */
    @PostMapping("/logout-all")
    public ResponseEntity<APIResponse<MessageResponse>> logoutAll(HttpServletRequest httpRequest) {
        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.logoutAllDevices(userContext.getEmail());
            log.info("User logged out from all devices: {}", userContext.getEmail());
            return APIResponseUtil.success(response, "Logged out from all devices successfully");
        } catch (Exception e) {
            log.error("Logout all failed for user {}: {}", userContext.getEmail(), e.getMessage());
            return APIResponseUtil.internalServerError("Logout all devices failed");
        }
    }

    // ================ TOKEN MANAGEMENT ================

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<APIResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Token refresh attempt");

        try {
            AuthResponse response = authService.refreshToken(request);
            log.info("Token refreshed successfully");
            return APIResponseUtil.success(response, "Token refreshed successfully");
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return APIResponseUtil.unauthorized(e.getMessage());
        }
    }

    /**
     * Validate token
     */
    @PostMapping("/validate-token")
    public ResponseEntity<APIResponse<TokenValidationResponse>> validateToken(
            @Valid @RequestBody TokenValidationRequest request) {

        try {
            TokenValidationResponse response = authService.validateToken(request.getToken());
            return APIResponseUtil.success(response, "Token validation completed");
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Revoke specific token
     */
    @PostMapping("/revoke-token")
    public ResponseEntity<APIResponse<MessageResponse>> revokeToken(
            @Valid @RequestBody RevokeTokenRequest request,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.revokeToken(request);
            return APIResponseUtil.success(response, "Token revoked successfully");
        } catch (Exception e) {
            log.error("Token revocation failed: {}", e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Revoke all tokens for user
     */
    @PostMapping("/revoke-all-tokens")
    public ResponseEntity<APIResponse<MessageResponse>> revokeAllTokens(HttpServletRequest httpRequest) {
        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.revokeAllTokens(userContext.getEmail());
            return APIResponseUtil.success(response, "All tokens revoked successfully");
        } catch (Exception e) {
            log.error("Revoke all tokens failed: {}", e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ PASSWORD MANAGEMENT ================

    /**
     * Forgot password step 1 - Send OTP
     */
    @PostMapping("/forgot-password/step1")
    public ResponseEntity<APIResponse<MessageResponse>> forgotPasswordStep1(
            @Valid @RequestBody ForgotPasswordStep1Request request) {

        log.info("Forgot password step 1 for email: {}", request.getEmail());

        try {
            MessageResponse response = authService.forgotPasswordStep1(request);
            return APIResponseUtil.success(response, "OTP sent to your email");
        } catch (Exception e) {
            log.error("Forgot password step 1 failed for {}: {}", request.getEmail(), e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Forgot password step 2 - Verify OTP
     */
    @PostMapping("/forgot-password/step2")
    public ResponseEntity<APIResponse<TokenResponse>> forgotPasswordStep2(
            @Valid @RequestBody ForgotPasswordStep2Request request) {

        log.info("Forgot password step 2 for email: {}", request.getEmail());

        try {
            TokenResponse response = authService.forgotPasswordStep2(request);
            return APIResponseUtil.success(response, "OTP verified. Use the token to reset password.");
        } catch (Exception e) {
            log.error("Forgot password step 2 failed for {}: {}", request.getEmail(), e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("Password reset attempt");

        try {
            MessageResponse response = authService.resetPassword(request);
            log.info("Password reset successful");
            return APIResponseUtil.success(response, "Password reset successfully");
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Change password for authenticated user
     */
    @PostMapping("/change-password")
    public ResponseEntity<APIResponse<MessageResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.changePassword(request, userContext.getEmail());
            return APIResponseUtil.success(response, "Password changed successfully");
        } catch (Exception e) {
            log.error("Password change failed: {}", e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Validate password strength
     */
    @PostMapping("/validate-password")
    public ResponseEntity<APIResponse<MessageResponse>> validatePassword(
            @Valid @RequestBody PasswordValidationRequest request) {

        try {
            MessageResponse response = authService.validatePassword(request.getPassword());
            return APIResponseUtil.success(response, "Password validation completed");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ EMAIL VERIFICATION ================

    /**
     * Verify email with OTP
     */
    @PostMapping("/verify-email")
    public ResponseEntity<APIResponse<AuthResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        log.info("Email verification attempt for: {}", request.getEmail());

        try {
            AuthResponse response = authService.verifyEmail(request);
            log.info("Email verified successfully: {}", request.getEmail());
            return APIResponseUtil.success(response, "Email verified successfully");
        } catch (Exception e) {
            log.error("Email verification failed for {}: {}", request.getEmail(), e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<APIResponse<MessageResponse>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {

        log.info("Resend verification for email: {}", request.getEmail());

        try {
            MessageResponse response = authService.resendVerificationEmail(request);
            return APIResponseUtil.success(response, "Verification email sent");
        } catch (Exception e) {
            log.error("Resend verification failed for {}: {}", request.getEmail(), e.getMessage());
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Send verification OTP
     */
    @PostMapping("/send-verification-otp")
    public ResponseEntity<APIResponse<MessageResponse>> sendVerificationOtp(
            @Valid @RequestBody SendOtpRequest request) {

        try {
            MessageResponse response = authService.sendVerificationOtp(request.getEmail());
            return APIResponseUtil.success(response, "Verification OTP sent");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ TWO-FACTOR AUTHENTICATION ================

    /**
     * Enable two-factor authentication
     */
    @PostMapping("/2fa/enable")
    public ResponseEntity<APIResponse<MessageResponse>> enableTwoFactorAuth(HttpServletRequest httpRequest) {
        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.enableTwoFactorAuth(userContext.getEmail());
            return APIResponseUtil.success(response, "Two-factor authentication enabled");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Disable two-factor authentication
     */
    @PostMapping("/2fa/disable")
    public ResponseEntity<APIResponse<MessageResponse>> disableTwoFactorAuth(
            @Valid @RequestBody DisableTwoFactorRequest request,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.disableTwoFactorAuth(userContext.getEmail(), request.getCode());
            return APIResponseUtil.success(response, "Two-factor authentication disabled");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Generate backup codes for 2FA
     */
    @PostMapping("/2fa/backup-codes")
    public ResponseEntity<APIResponse<MessageResponse>> generateBackupCodes(HttpServletRequest httpRequest) {
        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.generateBackupCodes(userContext.getEmail());
            return APIResponseUtil.success(response, "Backup codes generated");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Verify two-factor code
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<APIResponse<TwoFactorResponse>> verifyTwoFactorCode(
            @Valid @RequestBody TwoFactorVerificationRequest request) {

        try {
            TwoFactorResponse response = authService.verifyTwoFactorCode(request);
            return APIResponseUtil.success(response, "Two-factor code verified");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ ACCOUNT MANAGEMENT ================

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<APIResponse<UserResponse>> getCurrentUser(HttpServletRequest httpRequest) {
        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            UserResponse response = authService.getCurrentUser(userContext.getEmail());
            return APIResponseUtil.success(response, "Profile retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.internalServerError("Failed to retrieve profile");
        }
    }

    /**
     * Check if email exists
     */
    @GetMapping("/check-email")
    public ResponseEntity<APIResponse<EmailCheckResponse>> checkEmail(
            @RequestParam String email) {

        if (!ValidationUtil.isValidEmail(email)) {
            return APIResponseUtil.badRequest("Invalid email format");
        }

        try {
            EmailCheckResponse response = authService.checkEmailExists(email);
            return APIResponseUtil.success(response, "Email check completed");
        } catch (Exception e) {
            return APIResponseUtil.internalServerError("Email check failed");
        }
    }

    /**
     * Get account status
     */
    @GetMapping("/account-status")
    public ResponseEntity<APIResponse<AccountStatusResponse>> getAccountStatus(
            @RequestParam String email) {

        try {
            AccountStatusResponse response = authService.getAccountStatus(email);
            return APIResponseUtil.success(response, "Account status retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Deactivate account
     */
    @PostMapping("/deactivate")
    public ResponseEntity<APIResponse<MessageResponse>> deactivateAccount(
            @Valid @RequestBody DeactivateAccountRequest request,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.deactivateAccount(userContext.getEmail(), request.getPassword());
            return APIResponseUtil.success(response, "Account deactivated successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Reactivate account
     */
    @PostMapping("/reactivate")
    public ResponseEntity<APIResponse<MessageResponse>> reactivateAccount(
            @Valid @RequestBody ReactivateAccountRequest request) {

        try {
            MessageResponse response = authService.reactivateAccount(request);
            return APIResponseUtil.success(response, "Account reactivated successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Request account deletion
     */
    @PostMapping("/request-deletion")
    public ResponseEntity<APIResponse<MessageResponse>> requestAccountDeletion(HttpServletRequest httpRequest) {
        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.requestAccountDeletion(userContext.getEmail());
            return APIResponseUtil.success(response, "Account deletion requested");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Confirm account deletion
     */
    @PostMapping("/confirm-deletion")
    public ResponseEntity<APIResponse<MessageResponse>> confirmAccountDeletion(
            @Valid @RequestBody ConfirmAccountDeletionRequest request) {

        try {
            MessageResponse response = authService.confirmAccountDeletion(request);
            return APIResponseUtil.success(response, "Account deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ SESSION MANAGEMENT ================

    /**
     * Get active sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<APIResponse<List<UserSessionResponse>>> getActiveSessions(HttpServletRequest httpRequest) {
        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<UserSessionResponse> response = authService.getActiveSessions(userContext.getEmail());
            return APIResponseUtil.success(response, "Active sessions retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Terminate specific session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<APIResponse<MessageResponse>> terminateSession(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.terminateSession(userContext.getEmail(), sessionId);
            return APIResponseUtil.success(response, "Session terminated");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Terminate all other sessions except current
     */
    @PostMapping("/sessions/terminate-others")
    public ResponseEntity<APIResponse<MessageResponse>> terminateAllOtherSessions(
            @RequestParam String currentSessionId,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.terminateAllOtherSessions(userContext.getEmail(), currentSessionId);
            return APIResponseUtil.success(response, "All other sessions terminated");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ DEVICE MANAGEMENT ================

    /**
     * Get registered devices
     */
    @GetMapping("/devices")
    public ResponseEntity<APIResponse<List<DeviceResponse>>> getRegisteredDevices(HttpServletRequest httpRequest) {
        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            List<DeviceResponse> response = authService.getRegisteredDevices(userContext.getEmail());
            return APIResponseUtil.success(response, "Registered devices retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Register new device
     */
    @PostMapping("/devices/register")
    public ResponseEntity<APIResponse<MessageResponse>> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.registerDevice(userContext.getEmail(), request);
            return APIResponseUtil.success(response, "Device registered successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Revoke device access
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<APIResponse<MessageResponse>> revokeDevice(
            @PathVariable String deviceId,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            MessageResponse response = authService.revokeDevice(userContext.getEmail(), deviceId);
            return APIResponseUtil.success(response, "Device access revoked");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ SECURITY & MONITORING ================

    /**
     * Get security logs
     */
    @GetMapping("/security-logs")
    public ResponseEntity<APIResponse<Page<SecurityLogResponse>>> getSecurityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {

        UserContextService.UserContext userContext = userContextService.extractUserContext(httpRequest);
        if (userContext == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<SecurityLogResponse> response = authService.getSecurityLogs(userContext.getEmail(), pageable);
            return APIResponseUtil.success(response, "Security logs retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Report suspicious activity
     */
    @PostMapping("/security/report")
    public ResponseEntity<APIResponse<MessageResponse>> reportSuspiciousActivity(
            @Valid @RequestBody SecurityReportRequest request) {

        try {
            MessageResponse response = authService.reportSuspiciousActivity(request);
            return APIResponseUtil.success(response, "Suspicious activity reported");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Check rate limit
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<APIResponse<MessageResponse>> checkRateLimit(
            @RequestParam String action,
            HttpServletRequest httpRequest) {

        try {
            String identifier = getClientIpAddress(httpRequest);
            MessageResponse response = authService.checkRateLimit(identifier, action);
            return APIResponseUtil.success(response, "Rate limit check completed");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ ADMIN ENDPOINTS ================

    /**
     * Get all users (Admin only)
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<UserResponse> response = authService.getAllUsers(pageable, search, role, isActive);
            return APIResponseUtil.success(response, "Users retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.internalServerError("Failed to retrieve users");
        }
    }

    /**
     * Admin reset user password
     */
    @PostMapping("/admin/reset-password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> adminResetPassword(
            @Valid @RequestBody AdminResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            MessageResponse response = authService.adminResetPassword(request);
            return APIResponseUtil.success(response, "Password reset successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Admin unlock user account
     */
    @PostMapping("/admin/unlock-account/{userEmail}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> adminUnlockAccount(
            @PathVariable String userEmail,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            MessageResponse response = authService.adminUnlockAccount(userEmail);
            return APIResponseUtil.success(response, "Account unlocked successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Admin deactivate user
     */
    @PostMapping("/admin/deactivate/{userEmail}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> adminDeactivateUser(
            @PathVariable String userEmail,
            @RequestParam String reason,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            MessageResponse response = authService.adminDeactivateUser(userEmail, reason);
            return APIResponseUtil.success(response, "User deactivated successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Admin activate user
     */
    @PostMapping("/admin/activate/{userEmail}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> adminActivateUser(
            @PathVariable String userEmail,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            MessageResponse response = authService.adminActivateUser(userEmail);
            return APIResponseUtil.success(response, "User activated successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get user statistics
     */
    @GetMapping("/admin/user-stats/{userEmail}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<UserStatsResponse>> getUserStats(
            @PathVariable String userEmail,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            UserStatsResponse response = authService.getUserStats(userEmail);
            return APIResponseUtil.success(response, "User statistics retrieved");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get all login history (Admin only)
     * Can optionally filter by user email
     */
    @GetMapping("/admin/login-history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<Page<AdminLoginHistoryItemResponse>>> getAllLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return APIResponseUtil.forbidden("Admin access required");
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("loginTime").descending());
            Page<AdminLoginHistoryItemResponse> response = authService.getAllLoginHistory(pageable, userEmail);
            return APIResponseUtil.success(response, "Login history retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Create or update a role with permissions (SUPER_ADMIN only).
     */
    @PostMapping("/admin/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> createOrUpdateRole(
            @Valid @RequestBody RoleRequest request) {

        try {
            MessageResponse response = authService.createOrUpdateRole(request);
            return APIResponseUtil.success(response, "Role saved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get all roles (SUPER_ADMIN only).
     */
    @GetMapping("/admin/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<List<RoleResponse>>> getAllRoles() {
        try {
            List<RoleResponse> response = authService.getAllRoles();
            return APIResponseUtil.success(response, "Roles retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Get specific role by ID (SUPER_ADMIN only).
     */
    @GetMapping("/admin/roles/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<RoleResponse>> getRoleById(@PathVariable String roleId) {
        try {
            RoleResponse response = authService.getRole(roleId);
            return APIResponseUtil.success(response, "Role retrieved successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Update a role (SUPER_ADMIN only).
     */
    @PutMapping("/admin/roles/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<RoleResponse>> updateRole(
            @PathVariable String roleId,
            @Valid @RequestBody RoleRequest request) {
        try {
            RoleResponse response = authService.updateRole(roleId, request);
            return APIResponseUtil.success(response, "Role updated successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Delete a role (SUPER_ADMIN only).
     */
    @DeleteMapping("/admin/roles/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> deleteRole(@PathVariable String roleId) {
        try {
            MessageResponse response = authService.deleteRole(roleId);
            return APIResponseUtil.success(response, "Role deleted successfully");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    /**
     * Assign roles to a user (SUPER_ADMIN only).
     */
    @PostMapping("/admin/users/assign-roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> assignRolesToUser(
            @Valid @RequestBody AssignRolesRequest request) {

        try {
            MessageResponse response = authService.assignRolesToUser(request);
            return APIResponseUtil.success(response, "Roles assigned to user");
        } catch (Exception e) {
            return APIResponseUtil.badRequest(e.getMessage());
        }
    }

    // ================ UTILITY METHODS ================

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

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<APIResponse<String>> healthCheck() {
        return APIResponseUtil.success("Auth service is running", "Service healthy");
    }
}