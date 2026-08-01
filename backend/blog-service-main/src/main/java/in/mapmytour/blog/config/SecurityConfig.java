package in.mapmytour.blog.config;

import in.mapmytour.blog.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GatewayOnlyAccessFilter gatewayOnlyAccessFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring SecurityFilterChain with JWT filter: {}", jwtAuthenticationFilter.getClass().getSimpleName());
        
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 🔐 Authenticated endpoints - require JWT (user-specific GET endpoints - must come before wildcards)
                        .requestMatchers("GET", "/api/v1/blog/posts/my-posts").authenticated()
                        .requestMatchers("GET", "/api/v1/blog/posts/my-likes").authenticated()
                        .requestMatchers("GET", "/api/v1/blog/comments/my-comments").authenticated()
                        .requestMatchers("GET", "/api/v1/blog/likes/my-likes").authenticated()
                        // 🔐 Admin-only GET endpoints (accepts both ADMIN and SUPER_ADMIN roles)
                        .requestMatchers("GET", "/api/v1/blog/settings/stats").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // ✅ Public GET endpoints - no authentication required
                        .requestMatchers("GET", "/api/v1/blog/posts/**").permitAll()
                        .requestMatchers("GET", "/api/v1/blog/categories/**").permitAll()
                        .requestMatchers("GET", "/api/v1/blog/tags/**").permitAll()
                        .requestMatchers("GET", "/api/v1/blog/settings/**").permitAll()
                        .requestMatchers("GET", "/api/v1/blog/comments/**").permitAll()
                        .requestMatchers("GET", "/api/v1/blog/likes/**").permitAll()
                        .requestMatchers("GET", "/api/v1/blog/media/**").permitAll()
                        // 📊 Standardized Actuator security: health/info public, others ADMIN
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN")
                        // 🔐 Authenticated endpoints - require JWT (POST, PUT, DELETE operations)
                        .requestMatchers("/api/v1/blog/posts/create").authenticated()
                        .requestMatchers("/api/v1/blog/posts/*/like").authenticated()
                        .requestMatchers("/api/v1/blog/posts/*/publish").authenticated()
                        .requestMatchers("/api/v1/blog/posts/*/unpublish").authenticated()
                        .requestMatchers("POST", "/api/v1/blog/comments").authenticated()
                        .requestMatchers("PUT", "/api/v1/blog/comments/*").authenticated()
                        .requestMatchers("DELETE", "/api/v1/blog/comments/*").authenticated()
                        .requestMatchers("POST", "/api/v1/blog/comments/*/approve").authenticated()
                        .requestMatchers("POST", "/api/v1/blog/comments/*/reject").authenticated()
                        .requestMatchers("POST", "/api/v1/blog/likes/**").authenticated()
                        .requestMatchers("DELETE", "/api/v1/blog/likes/**").authenticated()
                        .requestMatchers("POST", "/api/v1/blog/media/**").authenticated()
                        .requestMatchers("PUT", "/api/v1/blog/media/**").authenticated()
                        .requestMatchers("DELETE", "/api/v1/blog/media/**").authenticated()
                        // 🔐 Admin endpoints - require ADMIN role (POST, PUT, DELETE operations)
                        .requestMatchers("POST", "/api/v1/blog/categories").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/v1/blog/categories/*").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/v1/blog/categories/*").hasRole("ADMIN")
                        .requestMatchers("POST", "/api/v1/blog/tags").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/v1/blog/tags/*").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/v1/blog/tags/*").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/v1/blog/posts/*").authenticated()
                        .requestMatchers("DELETE", "/api/v1/blog/posts/*").authenticated()
                        .requestMatchers("DELETE", "/api/v1/blog/admin/**").hasRole("ADMIN")
                        .requestMatchers("POST", "/api/v1/blog/settings").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/v1/blog/settings/*").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/v1/blog/settings/*").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
            // Add gateway-only access filter first (blocks direct access)
            .addFilterBefore(gatewayOnlyAccessFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(parseCsv(allowedOrigins));
        configuration.setAllowedMethods(parseCsv(allowedMethods));
        configuration.setAllowedHeaders(parseCsv(allowedHeaders));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private List<String> parseCsv(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
