// src/main/java/in/mapmytour/auth/security/GatewayHeaderAuthenticationFilter.java
package in.mapmytour.auth.security;

import in.mapmytour.auth.service.UserContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private final UserContextService userContextService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // Skip if already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract user context from gateway headers
            UserContextService.UserContext userContext = userContextService.extractUserContext(request);

            if (userContext != null && userContext.isAuthenticated()) {
                log.debug("Setting authentication from gateway headers for user: {}", userContext.getEmail());

                // Create authorities from role
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + userContext.getRole())
                );

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userContext.getEmail(),
                                null,
                                authorities
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Successfully set authentication for user: {} with role: {}",
                        userContext.getEmail(), userContext.getRole());
            } else {
                log.debug("No valid gateway authentication found for request: {} {}",
                        request.getMethod(), request.getRequestURI());
            }
        } catch (Exception ex) {
            log.error("Error setting authentication from gateway headers: {}", ex.getMessage(), ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip OAuth2 endpoints as they have their own authentication
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            return true;
        }

        // Skip actuator endpoints
        if (path.startsWith("/actuator/")) {
            return true;
        }

        // Only process API endpoints
        return !path.startsWith("/api/");
    }
}