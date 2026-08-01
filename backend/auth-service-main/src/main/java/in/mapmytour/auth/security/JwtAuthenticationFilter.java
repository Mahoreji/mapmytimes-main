package in.mapmytour.auth.security;

import in.mapmytour.auth.service.RateLimitingService;
import in.mapmytour.auth.utils.IpAddressUtils;
import in.mapmytour.auth.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // Validate token format and signature
                if (!jwtUtil.validateToken(jwt)) {
                    log.debug("Invalid JWT token");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Check if it's an access token
                if (!jwtUtil.isAccessToken(jwt)) {
                    log.debug("JWT is not an access token");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Extract user information
                String username = jwtUtil.getUsernameFromToken(jwt);
                String sessionId = jwtUtil.getSessionIdFromToken(jwt);
                String deviceId = jwtUtil.getDeviceIdFromToken(jwt);

                // Additional security checks
                if (isTokenSuspicious(jwt, request)) {
                    log.warn("Suspicious token usage detected for user: {}", username);
                    String clientIp = getClientIpAddress(request);
                    rateLimitingService.blockTemporarily(clientIp, Duration.ofMinutes(30));
                    filterChain.doFilter(request, response);
                    return;
                }

                // Load user details and set authentication
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Add user context to request headers for downstream services
                    request.setAttribute("X-User-Id", jwtUtil.getUserIdFromToken(jwt));
                    request.setAttribute("X-User-Email", username);
                    request.setAttribute("X-Session-Id", sessionId);
                    request.setAttribute("X-Device-Id", deviceId);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);

            // Rate limit on authentication errors
            String clientIp = getClientIpAddress(request);
            String rateLimitKey = clientIp + ":auth_error";
            rateLimitingService.recordAttempt(rateLimitKey);

            if (rateLimitingService.isRateLimited(rateLimitKey, 10, Duration.ofMinutes(15))) {
                rateLimitingService.blockTemporarily(clientIp, Duration.ofMinutes(15));
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return jwtUtil.extractTokenFromHeader(bearerToken);
    }

    private boolean isTokenSuspicious(String jwt, HttpServletRequest request) {
        try {
            // IP validation disabled for Cloudflare/proxy environments
            // Cloudflare changes IPs between requests, making IP-based validation unreliable
            // Token signature and expiration are sufficient security measures
            
            // Check if token is expiring soon and might be from a replay attack
            String requestIp = getClientIpAddress(request);
            if (jwtUtil.isTokenExpiringSoon(jwt)) {
                String rateLimitKey = requestIp + ":expiring_token_usage";
                rateLimitingService.recordAttempt(rateLimitKey);

                if (rateLimitingService.isRateLimited(rateLimitKey, 20, Duration.ofMinutes(5))) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.debug("Error checking token suspicion: {}", e.getMessage());
            return false;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        return IpAddressUtils.getClientIpAddress(request);
    }
}