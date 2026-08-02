package in.mapmytour.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // DISABLED: CORS is handled by API Gateway's CorsWebFilter
        // When accessed through API Gateway, the gateway sets CORS headers
        // Setting CORS here causes duplicate Access-Control-Allow-Origin headers
        // This config is kept for backward compatibility but disabled
        // 
        // If direct access to auth service is needed (not through gateway),
        // uncomment and configure appropriately
        /*
        registry.addMapping("/**")
                .allowedOriginPatterns("https://mapmytimes.com", "https://www.mapmytimes.com", "https://staging.mapmytimes.com", "http://localhost:3000","http://localhost:3001","https://*.mapmytimes.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        */
    }
}
