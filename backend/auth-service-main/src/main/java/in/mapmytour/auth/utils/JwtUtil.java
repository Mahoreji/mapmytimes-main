package in.mapmytour.auth.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import in.mapmytour.auth.security.UserPrincipal;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:604800000}") // 7 days default
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private long jwtRefreshExpirationMs;

    @Value("${jwt.remember-me-expiration:2592000000}") // 30 days
    private long rememberMeExpirationMs;

    @Value("${jwt.password-reset-expiration:1800000}") // 30 minutes
    private long passwordResetExpirationMs;

    @Value("${jwt.email-verification-expiration:86400000}") // 24 hours
    private long emailVerificationExpirationMs;

    @Value("${jwt.issuer:auth-service}")
    private String jwtIssuer;

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String TOKEN_TYPE_PASSWORD_RESET = "password_reset";
    private static final String TOKEN_TYPE_EMAIL_VERIFICATION = "email_verification";
    private static final String TOKEN_TYPE_TWO_FACTOR = "two_factor";

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_DEVICE_ID = "deviceId";
    private static final String CLAIM_SESSION_ID = "sessionId";
    private static final String CLAIM_IP_ADDRESS = "ipAddress";
    private static final String CLAIM_USER_AGENT = "userAgent";
    private static final String CLAIM_IS_REMEMBER_ME = "rememberMe";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate access token with user details
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateAccessToken(userPrincipal);
    }

    /**
     * Generate access token with UserDetails
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        if (userDetails instanceof UserPrincipal) {
            claims.put(CLAIM_USER_ID, ((UserPrincipal) userDetails).getId());
        }
        claims.put(CLAIM_ROLES, userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return createToken(claims, userDetails.getUsername(), jwtExpirationMs);
    }

    /**
     * Generate access token with email
     */
    public String generateAccessToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        return createToken(claims, email, jwtExpirationMs);
    }

    /**
     * Generate access token with additional claims
     */
    public String generateAccessToken(String email, String userId, String name, String roles,
                                      String deviceId, String sessionId, String ipAddress,
                                      String userAgent, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_NAME, name);
        claims.put(CLAIM_ROLES, roles);
        claims.put(CLAIM_DEVICE_ID, deviceId);
        claims.put(CLAIM_SESSION_ID, sessionId);
        claims.put(CLAIM_IP_ADDRESS, ipAddress);
        claims.put(CLAIM_USER_AGENT, userAgent);
        claims.put(CLAIM_IS_REMEMBER_ME, rememberMe);

        long expiration = rememberMe ? rememberMeExpirationMs : jwtExpirationMs;
        return createToken(claims, email, expiration);
    }

    /**
     * Generate access token with list of roles (for RBAC support)
     */
    public String generateAccessToken(String email, String userId, String name, List<String> roles,
                                      String deviceId, String sessionId, String ipAddress,
                                      String userAgent, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_NAME, name);
        claims.put(CLAIM_ROLES, roles);
        claims.put(CLAIM_DEVICE_ID, deviceId);
        claims.put(CLAIM_SESSION_ID, sessionId);
        claims.put(CLAIM_IP_ADDRESS, ipAddress);
        claims.put(CLAIM_USER_AGENT, userAgent);
        claims.put(CLAIM_IS_REMEMBER_ME, rememberMe);

        long expiration = rememberMe ? rememberMeExpirationMs : jwtExpirationMs;
        return createToken(claims, email, expiration);
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        return createToken(claims, email, jwtRefreshExpirationMs);
    }

    /**
     * Generate refresh token with device info
     */
    public String generateRefreshToken(String email, String deviceId, String sessionId,
                                       String ipAddress, String userAgent, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        claims.put(CLAIM_DEVICE_ID, deviceId);
        claims.put(CLAIM_SESSION_ID, sessionId);
        claims.put(CLAIM_IP_ADDRESS, ipAddress);
        claims.put(CLAIM_USER_AGENT, userAgent);
        claims.put(CLAIM_IS_REMEMBER_ME, rememberMe);

        long expiration = rememberMe ? rememberMeExpirationMs : jwtRefreshExpirationMs;
        return createToken(claims, email, expiration);
    }

    /**
     * Generate email verification token
     */
    public String generateEmailVerificationToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_EMAIL_VERIFICATION);
        claims.put(CLAIM_EMAIL, email);
        return createToken(claims, email, emailVerificationExpirationMs);
    }

    /**
     * Generate password reset token
     */
    public String generatePasswordResetToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_PASSWORD_RESET);
        claims.put(CLAIM_EMAIL, email);
        return createToken(claims, email, passwordResetExpirationMs);
    }

    /**
     * Generate two-factor authentication token
     */
    public String generateTwoFactorToken(String email, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_TWO_FACTOR);
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_EMAIL, email);
        return createToken(claims, email, 300000); // 5 minutes
    }

    /**
     * Generate random secure token
     */
    public String generateSecureRandomToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Create JWT token with claims
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        String jti = UUID.randomUUID().toString(); // JWT ID for tracking

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(jwtIssuer)
                .setId(jti)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(CLAIM_USER_ID));
    }

    /**
     * Extract roles from token.
     *
     * NOTE: Historically we stored roles sometimes as a single String and sometimes
     * as a JSON array. To be backward compatible and avoid ClassCastException,
     * we normalize both representations to List<String>.
     */
    public List<String> getRolesFromToken(String token) {
        return getClaimFromToken(token, claims -> {
            Object rawRoles = claims.get(CLAIM_ROLES);

            if (rawRoles == null) {
                return Collections.emptyList();
            }

            if (rawRoles instanceof List<?>) {
                return ((List<?>) rawRoles).stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
            }

            if (rawRoles instanceof String rawString) {
                if (rawString.isBlank()) {
                    return Collections.emptyList();
                }
                // Support comma-separated roles: "ROLE_USER,ROLE_ADMIN"
                return Arrays.stream(rawString.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }

            // Fallback – unexpected type, but still expose as single-element list
            return Collections.singletonList(String.valueOf(rawRoles));
        });
    }

    /**
     * Extract device ID from token
     */
    public String getDeviceIdFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(CLAIM_DEVICE_ID));
    }

    /**
     * Extract session ID from token
     */
    public String getSessionIdFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(CLAIM_SESSION_ID));
    }

    /**
     * Extract IP address from token
     */
    public String getIpAddressFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(CLAIM_IP_ADDRESS));
    }

    /**
     * Extract user agent from token
     */
    public String getUserAgentFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(CLAIM_USER_AGENT));
    }

    /**
     * Check if token is remember me
     */
    public Boolean isRememberMeToken(String token) {
        return getClaimFromToken(token, claims -> (Boolean) claims.get(CLAIM_IS_REMEMBER_ME));
    }

    /**
     * Extract expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Extract issued at date from token
     */
    public Date getIssuedAtFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    /**
     * Extract JWT ID from token
     */
    public String getJwtIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getId);
    }

    /**
     * Extract token type from token
     */
    public String getTokenType(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(CLAIM_TOKEN_TYPE));
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(CLAIM_EMAIL));
    }

    /**
     * Extract specific claim from token
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Check if token will expire soon (within 5 minutes)
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            Date fiveMinutesFromNow = new Date(System.currentTimeMillis() + 300000);
            return expiration.before(fiveMinutesFromNow);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token with user details
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token type
     */
    public boolean validateTokenType(String token, String expectedType) {
        try {
            String tokenType = getTokenType(token);
            return expectedType.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check token types
     */
    public boolean isAccessToken(String token) {
        return validateTokenType(token, TOKEN_TYPE_ACCESS);
    }

    public boolean isRefreshToken(String token) {
        return validateTokenType(token, TOKEN_TYPE_REFRESH);
    }

    public boolean isEmailVerificationToken(String token) {
        return validateTokenType(token, TOKEN_TYPE_EMAIL_VERIFICATION);
    }

    public boolean isPasswordResetToken(String token) {
        return validateTokenType(token, TOKEN_TYPE_PASSWORD_RESET);
    }

    public boolean isTwoFactorToken(String token) {
        return validateTokenType(token, TOKEN_TYPE_TWO_FACTOR);
    }

    /**
     * Get expiration as LocalDateTime
     */
    public LocalDateTime getExpirationAsLocalDateTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            log.error("Error getting expiration from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get remaining validity time in milliseconds
     */
    public long getRemainingValidityTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return Math.max(0, expiration.getTime() - System.currentTimeMillis());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Extract token from Authorization header
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * Create token claims map
     */
    public Map<String, Object> createClaims(String tokenType, String userId, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, tokenType);
        if (userId != null) {
            claims.put(CLAIM_USER_ID, userId);
        }
        if (roles != null && !roles.isEmpty()) {
            claims.put(CLAIM_ROLES, roles);
        }
        return claims;
    }

    /**
     * Get token metadata
     */
    public Map<String, Object> getTokenMetadata(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("subject", claims.getSubject());
            metadata.put("issuedAt", claims.getIssuedAt());
            metadata.put("expiration", claims.getExpiration());
            metadata.put("jwtId", claims.getId());
            metadata.put("issuer", claims.getIssuer());
            metadata.put("tokenType", claims.get(CLAIM_TOKEN_TYPE));
            metadata.put("userId", claims.get(CLAIM_USER_ID));
            metadata.put("name", claims.get(CLAIM_NAME));
            metadata.put("roles", claims.get(CLAIM_ROLES));
            metadata.put("deviceId", claims.get(CLAIM_DEVICE_ID));
            metadata.put("sessionId", claims.get(CLAIM_SESSION_ID));
            metadata.put("isExpired", isTokenExpired(token));
            metadata.put("remainingTime", getRemainingValidityTime(token));
            return metadata;
        } catch (Exception e) {
            log.error("Error extracting token metadata: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Getters for expiration times
     */
    public long getExpirationTime() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationTime() {
        return jwtRefreshExpirationMs;
    }

    public long getRememberMeExpirationTime() {
        return rememberMeExpirationMs;
    }

    public long getPasswordResetExpirationTime() {
        return passwordResetExpirationMs;
    }

    public long getEmailVerificationExpirationTime() {
        return emailVerificationExpirationMs;
    }
}