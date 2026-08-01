package in.mapmytour.customer.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Feign Client Configuration
 * Configures Feign clients for service-to-service communication
 */
@Configuration
public class FeignConfig {

    /**
     * Request interceptor to forward headers from incoming requests
     * This ensures authentication and other headers are passed to downstream services
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) 
                    RequestContextHolder.getRequestAttributes();
                
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    
                    // Forward authentication headers
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        template.header("Authorization", authHeader);
                    }
                    
                    // Forward user context headers
                    String userId = request.getHeader("X-User-Id");
                    if (userId != null) {
                        template.header("X-User-Id", userId);
                    }
                    
                    String userEmail = request.getHeader("X-User-Email");
                    if (userEmail != null) {
                        template.header("X-User-Email", userEmail);
                    }
                    
                    String userRole = request.getHeader("X-User-Role");
                    if (userRole != null) {
                        template.header("X-User-Role", userRole);
                    }
                    
                    // Forward request source header
                    template.header("X-Request-Source", "customer-support-service");
                }
            }
        };
    }
}

