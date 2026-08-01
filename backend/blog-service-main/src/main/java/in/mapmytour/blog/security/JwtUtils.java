package in.mapmytour.blog.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * JWT utility class for validating tokens from auth service
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Extract email (subject) from JWT token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from JWT token
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extract role from JWT token
     * Handles both "role" (singular) and "roles" (plural) claims
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return extractBestRoleFromClaims(claims);
    }

    /**
     * Extract issuer from JWT token
     */
    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    /**
     * Extract expiration date from JWT token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract any claim from JWT token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from JWT token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            
            // Check if token is expired
            if (isTokenExpired(token)) {
                log.warn("JWT token is expired");
                return false;
            }
            
            // Check issuer
            if (!jwtIssuer.equals(claims.getIssuer())) {
                log.warn("JWT token has invalid issuer: {}", claims.getIssuer());
                return false;
            }
            
            // Check if required claims are present
            if (claims.getSubject() == null || claims.get("userId") == null) {
                log.warn("JWT token missing required claims");
                return false;
            }
            
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
            return false;
        } catch (SecurityException e) {
            log.warn("JWT token signature validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error validating JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token and extract user information
     */
    public JwtUserInfo validateAndExtractUserInfo(String token) {
        if (!validateToken(token)) {
            return null;
        }
        
        try {
            Claims claims = extractAllClaims(token);
            String role = extractBestRoleFromClaims(claims);
            
            return JwtUserInfo.builder()
                    .email(claims.getSubject())
                    .userId(claims.get("userId", String.class))
                    .role(role)
                    .isVerified(claims.get("isVerified", Boolean.class))
                    .build();
        } catch (Exception e) {
            log.error("Error extracting user info from JWT token: {}", e.getMessage());
            return null;
        }
    }

    private String extractBestRoleFromClaims(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof String roleStr) {
            String normalized = normalizeRoleValue(roleStr);
            return normalized != null ? normalized : claims.get("role", String.class);
        }
        if (rolesObj instanceof List<?> list) {
            List<String> roles = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
            String best = chooseHighestRole(roles);
            if (best != null) return best;
        }
        String role = claims.get("role", String.class);
        String normalized = normalizeRoleValue(role);
        return normalized != null ? normalized : role;
    }

    private String chooseHighestRole(List<String> roles) {
        boolean hasSuperAdmin = false;
        boolean hasAdmin = false;
        boolean hasEmployee = false;
        boolean hasAgent = false;
        boolean hasSupplier = false;
        boolean hasB2B = false;
        boolean hasUser = false;

        for (String r : roles) {
            String normalized = normalizeRoleValue(r);
            if (normalized == null) continue;
            switch (normalized) {
                case "SUPER_ADMIN" -> hasSuperAdmin = true;
                case "ADMIN" -> hasAdmin = true;
                case "EMPLOYEE" -> hasEmployee = true;
                case "AGENT" -> hasAgent = true;
                case "SUPPLIER" -> hasSupplier = true;
                case "B2B" -> hasB2B = true;
                case "USER" -> hasUser = true;
            }
        }

        if (hasSuperAdmin) return "SUPER_ADMIN";
        if (hasAdmin) return "ADMIN";
        if (hasEmployee) return "EMPLOYEE";
        if (hasAgent) return "AGENT";
        if (hasSupplier) return "SUPPLIER";
        if (hasB2B) return "B2B";
        if (hasUser) return "USER";

        for (String r : roles) {
            String normalized = normalizeRoleValue(r);
            if (normalized != null) return normalized;
        }
        return null;
    }

    private String normalizeRoleValue(String role) {
        if (role == null) return null;
        String value = role.trim();
        if (value.isEmpty()) return null;
        if (value.contains(",")) value = value.split(",")[0].trim();
        value = value.toUpperCase(Locale.ROOT);
        if (value.startsWith("ROLE_")) value = value.substring("ROLE_".length());
        return value.isEmpty() ? null : value;
    }


    /**
     * User information extracted from JWT token
     */
    @Data
    @Builder
    public static class JwtUserInfo {
        private String email;
        private String userId;
        private String role;
        private Boolean isVerified;
    }
}
