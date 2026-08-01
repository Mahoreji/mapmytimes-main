package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.auth.AuthResponse;
import in.mapmytour.auth.dto.auth.OAuth2LoginRequest;
import in.mapmytour.auth.entity.RefreshToken;
import in.mapmytour.auth.entity.Role;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.entity.User.UserRole;
// import in.mapmytour.auth.helper.EmailHelper;
import in.mapmytour.auth.repository.RefreshTokenRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

public class OAuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    // private EmailHelper emailHelper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private OAuthServiceImpl oAuthService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // Mock common behavior
        when(jwtUtil.getExpirationTime()).thenReturn(3600000L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(jwtUtil.generateRefreshToken(any(), any(), any(), any(), any(), anyBoolean())).thenReturn("refresh-token");
    }

    @Test
    void processOAuth2Login_ShouldIncludeAllRoles_WhenUserIsEmployee() {
        // Given
        String email = "employee@example.com";
        String userId = "user-123";

        // Create user with legacy role USER
        User user = User.builder()
                .id(userId)
                .email(email)
                .role(UserRole.USER)
                .provider("google")
                .isActive(true)
                .isVerified(true)
                .password("encoded-pwd")
                .loginAttempts(0)
                .build();

        // Add RBAC role EMPLOYEE
        Set<Role> rbacRoles = new HashSet<>();
        Role employeeRole = new Role();
        employeeRole.setName("EMPLOYEE");
        rbacRoles.add(employeeRole);
        user.setRoles(rbacRoles);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        OAuth2LoginRequest request = new OAuth2LoginRequest();
        request.setEmail(email);
        request.setProvider("google");
        request.setProviderId("google-123");
        request.setFirstName("Test");
        request.setLastName("Employee");
        request.setIpAddress("127.0.0.1");

        // When
        AuthResponse response = oAuthService.processOAuth2Login(request, httpRequest);

        // Then
        ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(jwtUtil).generateAccessToken(
                eq(email),
                eq(userId),
                anyString(), // name
                rolesCaptor.capture(), // Capture the roles list
                any(), // deviceId
                any(), // sessionId
                any(), // ipAddress
                any(), // userAgent
                eq(false) // rememberMe
        );

        List<String> capturedRoles = rolesCaptor.getValue();

        // Verify legacy roles are present
        assertTrue(capturedRoles.contains("USER"), "Should contain USER");
        assertTrue(capturedRoles.contains("ROLE_USER"), "Should contain ROLE_USER");

        // Verify RBAC roles are present
        assertTrue(capturedRoles.contains("EMPLOYEE"), "Should contain EMPLOYEE");
        assertTrue(capturedRoles.contains("ROLE_EMPLOYEE"), "Should contain ROLE_EMPLOYEE");
    }

    @Test
    void processOAuth2Login_ForTejasBanait_ShouldIncludeEmployeeRole() {
        // Given - Specific user data from the token
        String email = "tejasbanait456@gmail.com";
        String userId = "5a0f9912-0dff-4c39-88dd-22601a1b0389";

        // Create user with legacy role USER (simulating the bug state in DB)
        User user = User.builder()
                .id(userId)
                .email(email)
                .role(UserRole.USER)
                .provider("google")
                .isActive(true)
                .isVerified(true)
                .password("encoded-pwd")
                .loginAttempts(0)
                .build();

        // Add RBAC role EMPLOYEE (the missing role)
        Set<Role> rbacRoles = new HashSet<>();
        Role employeeRole = new Role();
        employeeRole.setName("EMPLOYEE");
        rbacRoles.add(employeeRole);
        user.setRoles(rbacRoles);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        OAuth2LoginRequest request = new OAuth2LoginRequest();
        request.setEmail(email);
        request.setProvider("google");
        request.setProviderId("google-123");
        request.setFirstName("Tejas");
        request.setLastName("Banait");
        request.setIpAddress("127.0.0.1");

        // When
        AuthResponse response = oAuthService.processOAuth2Login(request, httpRequest);

        // Then
        ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
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

        // Verify the fix works for this specific user
        assertTrue(capturedRoles.contains("USER"), "Should contain USER");
        assertTrue(capturedRoles.contains("EMPLOYEE"), "Should contain EMPLOYEE");
        assertTrue(capturedRoles.contains("ROLE_EMPLOYEE"), "Should contain ROLE_EMPLOYEE");
    }
}
