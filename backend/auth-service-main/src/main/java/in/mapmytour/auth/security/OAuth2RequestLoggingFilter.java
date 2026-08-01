package in.mapmytour.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs OAuth2 requests to help debug 401 errors
 */
@Component
@Order(1)
@Slf4j
public class OAuth2RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        
        // Only log OAuth2 related requests
        if (requestUri.contains("/oauth2/") || requestUri.contains("/login/oauth2/")) {
            log.info("=== OAuth2 Request ===");
            log.info("Method: {}", request.getMethod());
            log.info("Request URI: {}", requestUri);
            log.info("Query String: {}", request.getQueryString());
            log.info("Full URL: {}{}", requestUri, request.getQueryString() != null ? "?" + request.getQueryString() : "");
            
            // Log all headers
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (headerName.toLowerCase().contains("oauth") || 
                    headerName.toLowerCase().contains("authorization") ||
                    headerName.toLowerCase().contains("redirect")) {
                    log.info("Header {}: {}", headerName, request.getHeader(headerName));
                }
            }
            
            // Extract redirect_uri from query string if present
            String queryString = request.getQueryString();
            if (queryString != null && queryString.contains("redirect_uri")) {
                String redirectUri = java.util.Arrays.stream(queryString.split("&"))
                    .filter(param -> param.startsWith("redirect_uri="))
                    .findFirst()
                    .map(param -> {
                        try {
                            return java.net.URLDecoder.decode(param.substring("redirect_uri=".length()), java.nio.charset.StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            return param.substring("redirect_uri=".length());
                        }
                    })
                    .orElse("NOT_FOUND");
                log.info("Redirect URI from query: {}", redirectUri);
            }
            
            log.info("=====================");
        }
        
        filterChain.doFilter(request, response);
    }
}

