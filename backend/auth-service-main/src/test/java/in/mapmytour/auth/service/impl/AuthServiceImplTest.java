package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.auth.AuthResponse;
import in.mapmytour.auth.dto.auth.LoginWithOtpRequest;
import in.mapmytour.auth.entity.OtpToken;
import in.mapmytour.auth.entity.Role;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.entity.User.UserRole;
// import in.mapmytour.auth.helper.EmailHelper;
import in.mapmytour.auth.repository.LoginHistoryRepository;
import in.mapmytour.auth.repository.OtpTokenRepository;
import in.mapmytour.auth.repository.RefreshTokenRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.repository.RoleRepository;
import in.mapmytour.auth.repository.PermissionRepository;
import in.mapmytour.auth.repository.AgentRepository;
import in.mapmytour.auth.repository.SupplierRepository;
import in.mapmytour.auth.repository.VerificationRuleRepository;
import in.mapmytour.auth.repository.VerificationRequestRepository;
import in.mapmytour.auth.helper.GeoLocationHelper;
import in.mapmytour.auth.helper.S3Helper;
import in.mapmytour.auth.helper.RealtimeNotificationHelper;
import in.mapmytour.auth.service.NotificationService;
import in.mapmytour.auth.service.AutomatedVerificationService;
import in.mapmytour.auth.event.AuthEventProducer;
import in.mapmytour.auth.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthServiceImplTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private JwtUtil jwtUtil;

        @Mock
        private OtpTokenRepository otpTokenRepository;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private LoginHistoryRepository loginHistoryRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private PermissionRepository permissionRepository;

        @Mock
        private GeoLocationHelper geoLocationHelper;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private NotificationService notificationService;

        @Mock
        private AuthEventProducer authEventProducer;

        @Mock
        private S3Helper s3Helper;

        @Mock
        private AgentRepository agentRepository;

        @Mock
        private SupplierRepository supplierRepository;

        @Mock
        private VerificationRuleRepository verificationRuleRepository;

        @Mock
        private VerificationRequestRepository verificationRequestRepository;

        @Mock
        private AutomatedVerificationService automatedVerificationService;

        @Mock
        private RealtimeNotificationHelper realtimeNotificationHelper;

        @InjectMocks
        private AuthServiceImpl authService;

        @BeforeEach
        void setup() {
                MockitoAnnotations.openMocks(this);
                when(jwtUtil.getExpirationTime()).thenReturn(3600000L);
                when(jwtUtil.generateRefreshToken(any(), any(), any(), any(), any(), anyBoolean()))
                                .thenReturn("refresh-token");
                when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        }

        @Test
        void loginWithOtp_ForTejasBanait_ShouldIncludeEmployeeRole() {
                // Given
                String email = "tejasbanait456@gmail.com";
                String otp = "123456";
                String userId = "5a0f9912-0dff-4c39-88dd-22601a1b0389";

                // User setup
                User user = User.builder()
                                .id(userId)
                                .email(email)
                                .role(UserRole.USER)
                                .isActive(true)
                                .isVerified(true)
                                .build();

                Set<Role> rbacRoles = new HashSet<>();
                Role employeeRole = new Role();
                employeeRole.setName("EMPLOYEE");
                rbacRoles.add(employeeRole);
                user.setRoles(rbacRoles);

                // OTP Token setup
                OtpToken otpToken = OtpToken.builder()
                                .email(email)
                                .otp(otp)
                                .type(OtpToken.OtpType.LOGIN_VERIFICATION)
                                .expiresAt(LocalDateTime.now().plusMinutes(10))
                                .build();

                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(otpTokenRepository.findByEmailAndOtpAndType(email, otp, OtpToken.OtpType.LOGIN_VERIFICATION))
                                .thenReturn(Optional.of(otpToken));

                LoginWithOtpRequest request = new LoginWithOtpRequest();
                request.setEmail(email);
                request.setOtp(otp);

                // When
                AuthResponse response = authService.loginWithOtp(request);

                // Then
                org.junit.jupiter.api.Assertions.assertNotNull(response);

                @SuppressWarnings({ "unchecked", "rawtypes" })
                ArgumentCaptor<List<String>> rolesCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
                verify(jwtUtil).generateAccessToken(
                                eq(email),
                                eq(userId),
                                anyString(),
                                rolesCaptor.capture(),
                                any(),
                                any(),
                                any(),
                                any(),
                                eq(false));

                List<String> capturedRoles = rolesCaptor.getValue();
                assertTrue(capturedRoles.contains("EMPLOYEE"), "Should contain EMPLOYEE role");
                assertTrue(capturedRoles.contains("USER"), "Should contain USER role");
        }
}
