package in.mapmytour.auth.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private boolean isAuthenticated;
    private String email;
    private String accessToken;
    private UserResponse user;
    private AgentResponse agent;
    private SupplierResponse supplier;
    private String refreshToken;
    private long expiresIn;
    private String tokenType;
    private String sessionId;
    private String deviceId;
    private boolean requiresTwoFactor;
    private String twoFactorToken;
    private String provider; // Add for OAuth
}