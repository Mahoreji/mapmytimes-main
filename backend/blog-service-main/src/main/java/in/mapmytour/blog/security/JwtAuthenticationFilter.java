package in.mapmytour.blog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter for validating tokens from API Gateway
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.debug("JWT Filter processing request: {} with auth header: {}", request.getRequestURI(), authHeader != null ? "present" : "null");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found. Request passed gateway verification but is anonymous for user context.");
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // Validate JWT token and extract user info
            log.debug("Validating JWT token at service level...");
            JwtUtils.JwtUserInfo userInfo = jwtUtils.validateAndExtractUserInfo(token);
            
            if (userInfo != null) {
                log.debug("JWT validation successful for user: {}", userInfo.getEmail());
                
                // Create authentication object
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userInfo.getEmail(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userInfo.getRole()))
                );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                // Add user info to request attributes for UserContextService
                request.setAttribute("jwt.userId", userInfo.getUserId());
                request.setAttribute("jwt.email", userInfo.getEmail());
                request.setAttribute("jwt.role", userInfo.getRole());
                request.setAttribute("jwt.isVerified", userInfo.getIsVerified());
            } else {
                log.warn("JWT token validation failed - userInfo is null");
                sendUnauthorizedResponse(response, "Invalid or expired token");
                return;
            }
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage(), e);
            sendUnauthorizedResponse(response, "Token processing error");
            return;
        }

        filterChain.doFilter(request, response);
    }


    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"success\":false,\"statusCode\":401,\"message\":\"" + message + "\",\"data\":null,\"errors\":null}");
        response.getWriter().flush();
    }
}
