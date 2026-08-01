package in.mapmytour.api.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Component
public class GatewayJwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800000}") // 7 days default
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private long jwtRefreshExpirationMs;

    @Value("${jwt.issuer:auth-service}")
    private String jwtIssuer;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public String getTokenType(Claims claims) {
        return (String) claims.get("type");
    }

    public String getEmailFromClaims(Claims claims) {
        return (String) claims.get("email");
    }

    public String getUserIdFromClaims(Claims claims) {
        return (String) claims.get("userId");
    }

    public String getUserRoleFromClaims(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof java.util.List) {
            java.util.List<?> rolesList = (java.util.List<?>) rolesObj;
            if (rolesList.isEmpty()) {
                return null;
            }

            int highestPriority = Integer.MAX_VALUE;
            String highestRole = null;

            for (Object roleObj : rolesList) {
                if (roleObj == null)
                    continue;

                String roleStr = roleObj.toString().toUpperCase();
                if (roleStr.startsWith("ROLE_")) {
                    roleStr = roleStr.substring(5);
                }

                int priority = getRolePriority(roleStr);
                if (priority < highestPriority) {
                    highestPriority = priority;
                    highestRole = roleStr;
                }
            }
            return highestRole;
        }

        if (rolesObj != null) {
            String roleStr = rolesObj.toString().toUpperCase().trim();
            // Handle if it's a string representation of a list like "[ADMIN, USER]"
            if (roleStr.startsWith("[") && roleStr.endsWith("]")) {
                roleStr = roleStr.substring(1, roleStr.length() - 1);
                String[] parts = roleStr.split(",");
                int highestPriority = Integer.MAX_VALUE;
                String highestRole = null;
                for (String part : parts) {
                    String r = part.trim();
                    if (r.startsWith("ROLE_")) r = r.substring(5);
                    int priority = getRolePriority(r);
                    if (priority < highestPriority) {
                        highestPriority = priority;
                        highestRole = r;
                    }
                }
                return highestRole;
            }
            if (roleStr.startsWith("ROLE_")) {
                return roleStr.substring(5);
            }
            return roleStr;
        }
        return null;
    }

    private int getRolePriority(String role) {
        if (role == null)
            return 99;
        switch (role) {
            case "SUPER_ADMIN":
                return 1;
            case "ADMIN":
                return 2;
            case "B2B":
                return 3;
            case "SUPPLIER":
                return 4;
            case "AGENT":
                return 5;
            case "EMPLOYEE":
                return 6;
            case "USER":
                return 8;
            default:
                return 7; // Dynamic/Unknown roles have higher priority than base USER
        }
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .requireIssuer(jwtIssuer)
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
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return isTokenExpired(claims);
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return isAccessToken(claims);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(getTokenType(claims));
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return "refresh".equals(getTokenType(claims));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEmailVerificationToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return "email_verification".equals(getTokenType(claims));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPasswordResetToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return "password_reset".equals(getTokenType(claims));
        } catch (Exception e) {
            return false;
        }
    }

    public LocalDateTime getExpirationAsLocalDateTime(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return getExpirationAsLocalDateTime(claims);
        } catch (Exception e) {
            log.error("Error getting expiration from token: {}", e.getMessage());
            return null;
        }
    }

    public LocalDateTime getExpirationAsLocalDateTime(Claims claims) {
        try {
            Date expiration = claims.getExpiration();
            return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            log.error("Error getting expiration from claims: {}", e.getMessage());
            return null;
        }
    }

    public long getExpirationTime() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationTime() {
        return jwtRefreshExpirationMs;
    }

    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * Validates if the token is valid for API access
     * Checks if token is access token, not expired, and issued by auth-service
     */
    public boolean isValidForApiAccess(Claims claims) {
        try {
            // Check if it's an access token
            if (!"access".equals(claims.get("type"))) {
                log.warn("Token is not an access token. Type: {}", claims.get("type"));
                return false;
            }

            // Verify issuer matches auth-service
            String issuer = claims.getIssuer();
            if (issuer == null || !jwtIssuer.equals(issuer)) {
                log.warn("Token issuer mismatch. Expected: {}, Got: {}", jwtIssuer, issuer);
                return false;
            }

            log.debug("Token is valid for API access for user: {}", claims.getSubject());
            return true;
        } catch (Exception e) {
            log.error("Token validation failed for API access: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user information from Claims for request context
     */
    public UserContext extractUserContext(Claims claims) {
        try {
            UserContext context = new UserContext();
            context.setUserId(getUserIdFromClaims(claims));
            context.setEmail(claims.getSubject());
            context.setName((String) claims.get("name"));
            context.setRole(getUserRoleFromClaims(claims));
            return context;
        } catch (Exception e) {
            log.error("Error extracting user context from claims: {}", e.getMessage());
            return null;
        }
    }

    /**
     * User context class to hold user information from JWT
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserContext {
        private String userId;
        private String email;
        private String name;
        private String role;
    }
}
