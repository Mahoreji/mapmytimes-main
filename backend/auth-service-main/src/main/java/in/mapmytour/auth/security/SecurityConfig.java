// src/main/java/in/mapmytour/auth/security/SecurityConfig.java
package in.mapmytour.auth.security;

import in.mapmytour.auth.config.GatewayOnlyAccessFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import in.mapmytour.auth.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import in.mapmytour.auth.filter.ApiContentTypeFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final UserDetailsService userDetailsService;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
        private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;
        private final GatewayOnlyAccessFilter gatewayOnlyAccessFilter;
        private final ApiContentTypeFilter apiContentTypeFilter;
        private final OAuth2RequestLoggingFilter oAuth2RequestLoggingFilter;
        private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

        @Value("${app.cors.allowed-origins:http://localhost:3000}")
        private String[] allowedOrigins;

        @Value("${app.security.content-security-policy:default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'}")
        private String contentSecurityPolicy;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
                return new JwtAuthenticationFilter();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                authProvider.setHideUserNotFoundExceptions(false);
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public SecurityHeadersFilter securityHeadersFilter() {
                return new SecurityHeadersFilter();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:3001",
                                "http://localhost:8080",
                                "https://mapmytimes.com",
                                "https://www.mapmytimes.com",
                                "https://staging.mapmytimes.com",
                                "https://*.mapmytimes.com",
                                "https://mapmytimes.com",
                                "https://www.mapmytimes.com",
                                "https://staging.mapmytimes.com",
                                "https://api.mapmytimes.com",
                                "https://*.mapmytimes.com"));

                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setExposedHeaders(List.of("Authorization", "X-Total-Count", "X-Rate-Limit-Remaining"));
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        @Order(1)
        public SecurityFilterChain oAuth2SecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                                .addFilterBefore(oAuth2RequestLoggingFilter,
                                                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                                // DISABLED: CORS handled by API Gateway to prevent duplicate headers
                                // .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .headers(headers -> headers
                                                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                                                .contentTypeOptions(Customizer.withDefaults()))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(authorization -> authorization
                                                                .baseUri("/oauth2/authorization")
                                                                .authorizationRequestRepository(
                                                                                httpCookieOAuth2AuthorizationRequestRepository))
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri("/oauth2/callback/*"))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(jwtAccessDeniedHandler));

                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                // DISABLED: CORS handled by API Gateway to prevent duplicate headers
                                // .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .headers(headers -> headers
                                                .frameOptions(frameOptions -> frameOptions.deny())
                                                .contentTypeOptions(Customizer.withDefaults())
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .maxAgeInSeconds(31536000)
                                                                .includeSubDomains(true)
                                                                .preload(true))
                                                .referrerPolicy(referrer -> referrer.policy(
                                                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                .addHeaderWriter((request, response) -> {
                                                        response.setHeader("Content-Security-Policy",
                                                                        contentSecurityPolicy);
                                                        response.setHeader("X-Content-Type-Options", "nosniff");
                                                        response.setHeader("X-XSS-Protection", "1; mode=block");
                                                        response.setHeader("Permissions-Policy",
                                                                        "geolocation=(), microphone=(), camera=()");
                                                }))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(jwtAccessDeniedHandler))
                                .authorizeHttpRequests(authz -> authz
                                                // ================ PUBLIC ENDPOINTS (No Authentication Required)
                                                // ================

                                                // Authentication & Registration
                                                .requestMatchers(HttpMethod.GET, "/api/v1/auth/oauth2/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/oauth2/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/agent")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/supplier")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/send-otp").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login-otp").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/validate-token")
                                                .permitAll()

                                                // Password Management (Public)
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password/step1")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password/step2")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/validate-password")
                                                .permitAll()

                                                // Email Verification (Public)
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/verify-email")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/resend-verification")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/send-verification-otp")
                                                .permitAll()

                                                // Two Factor Authentication (Verification only)
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/2fa/verify").permitAll()

                                                // Account Management (Public queries)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/auth/check-email").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/auth/account-status")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/reactivate").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/confirm-deletion")
                                                .permitAll()

                                                // Security & Monitoring (Public reporting)
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/security/report")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/auth/rate-limit").permitAll()

                                                // Health check
                                                .requestMatchers(HttpMethod.GET, "/api/v1/auth/health").permitAll()

                                                // WebSocket endpoints - SockJS handshake and WebSocket connections
                                                // /ws/info is used by SockJS for handshake, must be public
                                                .requestMatchers("/ws/**", "/ws", "/api/v1/auth/ws/**", "/api/v1/auth/ws").permitAll()

                                                // User Profile & Search (Public - NOTE: Specific endpoint patterns
                                                // only)
                                                // GET /api/v1/user/profile/{userId}       - public user profiles by ID
                                                // GET /api/v1/user/profile/{userId}/connections - public connections
                                                // GET /api/v1/user/profile (no userId)    - REQUIRES AUTH (own profile)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/user/profile/{userId}").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/user/profile/{userId}/connections").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/user/search").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/user/interests/available")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/user/health").permitAll()
                                                
                                                // Internal Service Endpoints (Protected by Gateway/Internal Signature)
                                                .requestMatchers("/api/v1/user/internal/**").permitAll()

                                                // ================ ADMIN ENDPOINTS (Admin/Super Admin Only)
                                                // ================
                                                .requestMatchers("/api/v1/auth/admin/**")
                                                .hasAnyRole("ADMIN", "SUPER_ADMIN")
                                                .requestMatchers("/api/v1/user/admin/**")
                                                .hasAnyRole("ADMIN", "SUPER_ADMIN")

                                                // ================ PRIVATE ENDPOINTS (Authentication Required)
                                                // ================
                                                // All other auth endpoints require authentication
                                                .requestMatchers("/api/v1/auth/**").authenticated()

                                                // All other user endpoints require authentication
                                                .requestMatchers("/api/v1/user/**").authenticated()

                                                // Default: all other API endpoints require authentication
                                                .requestMatchers("/api/**").authenticated()

                                                // Allow everything else
                                                .anyRequest().permitAll())
                                // First authenticate via JWT (direct calls)
                                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                                // Then, if still not authenticated, authenticate via gateway headers
                                .addFilterBefore(gatewayHeaderAuthenticationFilter, JwtAuthenticationFilter.class)
                                // Force content-type mapping on API calls
                                .addFilterBefore(apiContentTypeFilter, UsernamePasswordAuthenticationFilter.class)
                                // Standardize: Force Gateway-only access for all API calls
                                .addFilterBefore(gatewayOnlyAccessFilter, UsernamePasswordAuthenticationFilter.class)
                                // Finally, apply security headers
                                .addFilterBefore(securityHeadersFilter(), UsernamePasswordAuthenticationFilter.class);

                http.authenticationProvider(authenticationProvider());

                return http.build();
        }

        @Bean
        @Order(3)
        public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/actuator/**")
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                                                .requestMatchers("/actuator/**").hasRole("ADMIN"))
                                .httpBasic(Customizer.withDefaults())
                                .addFilterBefore(gatewayOnlyAccessFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        @Order(4)
        public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                // DISABLED: CORS handled by API Gateway to prevent duplicate headers
                                // .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers("/", "/error", "/favicon.ico", "/swagger-ui/**",
                                                                "/v3/api-docs/**", "/swagger-ui.html")
                                                .permitAll()
                                                .anyRequest().permitAll());

                return http.build();
        }
}