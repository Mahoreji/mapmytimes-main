package in.mapmytour.api.config;

import in.mapmytour.api.filter.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import io.netty.resolver.DefaultAddressResolverGroup;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class GatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfig.class);

    private final AuthenticationFilter authenticationFilter;

    @Value("${spring.application.name:api-gateway-service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    // Service URLs from environment variables
    @Value("${AUTH_USER_SERVICE_URL:https://auth.mapmytimes.com}")
    private String authUserServiceUrl;

    @Value("${PAYMENT_SERVICE_URL:https://payment.mapmytimes.com}")
    private String paymentServiceUrl;

    @Value("${BOOKING_SERVICE_URL:https://booking.mapmytimes.com}")
    private String bookingServiceUrl;

    @Value("${TRAVEL_SERVICE_URI:${TRAVEL_SERVICE_URL:https://travel.mapmytimes.com}}")
    private String travelServiceUrl;

    @Value("${REVIEWS_SERVICE_URL:https://reviews.mapmytimes.com}")
    private String reviewsServiceUrl;

    @Value("${BLOG_SERVICE_URL:https://blog.mapmytimes.com}")
    private String blogServiceUrl;

    @Value("${CUSTOMER_SUPPORT_SERVICE_URL:https://customer.mapmytimes.com}")
    private String customerSupportServiceUrl;

    @Value("${UTILS_SERVICE_URL:https://utils.mapmytimes.com}")
    private String utilsServiceUrl;

    @Value("${CORE_SERVICE_URL:https://core.mapmytimes.com}")
    private String coreServiceUrl;

    @Value("${CHAT_SERVICE_URL:https://chat.mapmytimes.com}")
    private String chatServiceUrl;

    // CRM/ERP Service URLs
    @Value("${EMPLOYEE_SERVICE_URL:http://localhost:8091}")
    private String employeeServiceUrl;

    @Value("${AGENT_SERVICE_URL:http://localhost:8103}")
    private String agentServiceUrl;

    @Value("${LEAD_SERVICE_URL:http://localhost:8100}")
    private String leadServiceUrl;

    @Value("${SUPPLIER_SERVICE_URL:http://localhost:8094}")
    private String supplierServiceUrl;

    @Value("${WALLET_SERVICE_URL:http://localhost:8095}")
    private String walletServiceUrl;

    @Value("${COMMISSION_SERVICE_URL:http://localhost:8095}")
    private String commissionServiceUrl;

    @Value("${SUPPLIER_SETTLEMENT_SERVICE_URL:http://localhost:8096}")
    private String supplierSettlementServiceUrl;

    @Value("${GST_SERVICE_URL:http://localhost:8097}")
    private String gstServiceUrl;

    @Value("${FRAUD_SERVICE_URL:http://localhost:8098}")
    private String fraudServiceUrl;

    @Value("${DOCUMENT_SERVICE_URL:http://localhost:8099}")
    private String documentServiceUrl;

    @Value("${AUDIT_SERVICE_URL:http://localhost:8100}")
    private String auditServiceUrl;

    @Value("${REPORT_SERVICE_URL:http://localhost:8101}")
    private String reportServiceUrl;

    public GatewayConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
        log.info("Initializing Gateway Configuration for {}", applicationName);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Creating custom route locator for API Gateway");

        return builder.routes()
                // Note: /actuator/** endpoints are handled directly by Spring Boot Actuator
                // and should NOT be routed through the gateway to avoid loops
                // /fallback/** is handled directly by FallbackController, not routed through
                // gateway
                // to avoid infinite loops when circuit breakers trigger fallbacks

                .build();
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        log.info("Configuring security filter chain");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/health", "/actuator/**", "/fallback/**").permitAll()
                        .pathMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh").permitAll()
                        .pathMatchers("/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
                        .pathMatchers("/api/v1/auth/verify-email", "/api/v1/auth/resend-verification").permitAll()
                        .pathMatchers("/api/v1/auth/oauth2/**").permitAll()
                        .pathMatchers("/api/v1/utils/**").permitAll()
                        .pathMatchers("/api/v1/bookings/**").permitAll()
                        .pathMatchers("/api/v1/tours/**", "/api/v1/group-tours/**", "/api/v1/destinations/**").permitAll()
                        .pathMatchers("/api/v1/activities/**", "/api/v1/adventures/**").permitAll()
                        .pathMatchers("/api/v1/reviews/search", "/api/v1/reviews/entity/*").permitAll()
                        .pathMatchers("/api/v1/blog/posts/search", "/api/v1/blog/posts/slug/**").permitAll()
                        .pathMatchers("/api/v1/customer/knowledge-base/search").permitAll()

                        // All other endpoints require authentication
                        .anyExchange().permitAll() // Gateway handles auth via filters
                )
                .headers(headers -> headers.disable())
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        log.info("Configuring CORS web filter - single source of truth for CORS headers");

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "https://mapmytimes.com",
                "https://www.mapmytimes.com",
                "https://*.mapmytimes.com"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-User-Id",
                "X-User-Email",
                "X-User-Role",
                "X-Authenticated",
                "X-Request-Source"));
        corsConfig.setExposedHeaders(Arrays.asList(
                "X-Total-Count",
                "X-Page-Count",
                "X-Current-Page",
                "X-Request-Id",
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"));
        corsConfig.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Register CORS for all paths
        source.registerCorsConfiguration("/**", corsConfig);
        // WebSocket-specific CORS configuration
        CorsConfiguration wsCorsConfig = new CorsConfiguration(corsConfig);
        wsCorsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        source.registerCorsConfiguration("/auth/ws/**", wsCorsConfig);

        return new CorsWebFilter(source);
    }

    // Bean for logging service URLs on startup
    @Bean
    public ServiceUrlLogger serviceUrlLogger() {
        return new ServiceUrlLogger();
    }

    // StatusAggregator bean required by Resilience4j CircuitBreakersHealthIndicator
    @Bean
    public StatusAggregator statusAggregator() {
        return new SimpleStatusAggregator();
    }

    // Netty Server Customizer to increase max header size
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyServerCustomizer() {
        return factory -> {
            factory.addServerCustomizers(httpServer -> httpServer
                    .httpRequestDecoder(httpRequestDecoderSpec -> httpRequestDecoderSpec.maxHeaderSize(65536) // 64KB
                                                                                                              // instead
                                                                                                              // of
                                                                                                              // default
                                                                                                              // 8KB
            ));
            log.info(" Configured Netty HTTP server max header size to 64KB");
        };
    }

    // Custom HttpClient bean to fix localhost connection issues and IPv6 resolution
    // errors
    // System properties (java.net.preferIPv4Stack=true) are set in
    // ApiGatewayServiceApplication
    // This ensures IPv4 is preferred, avoiding [::1] IPv6 localhost resolution
    // errors
    @Bean
    public HttpClient httpClient() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("gateway-http-client")
                .maxConnections(1000)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofSeconds(300))
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // DefaultAddressResolverGroup respects java.net.preferIPv4Stack system property
        // Set in ApiGatewayServiceApplication.main() to avoid IPv6 [::1] localhost
        // errors
        // Docker service names (e.g., core-service:8083) resolve correctly via network
        // DNS
        return HttpClient.create(connectionProvider)
                .resolver(DefaultAddressResolverGroup.INSTANCE) // Respects IPv4 preference system property
                .responseTimeout(Duration.ofSeconds(300)) // Increased to 5 minutes for long-lived connections
                                                          // (WebSocket streaming)
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
        // Removed ReadTimeoutHandler and WriteTimeoutHandler for WebSocket streaming
        // These timeouts break long-lived streaming connections
        // The response-timeout in application.yml handles request-level timeouts
        ;
    }

    public class ServiceUrlLogger {
        public ServiceUrlLogger() {
            logServiceUrls();
        }

        private void logServiceUrls() {
            log.info("=".repeat(80));
            log.info("MapMyTour API Gateway - Service URL Configuration");
            log.info("=".repeat(80));
            log.info(" Auth & User Service:     {}", authUserServiceUrl);
            log.info(" Payment Service:         {}", paymentServiceUrl);
            log.info(" Booking Service:         {}", bookingServiceUrl);
            log.info(" Travel Service:          {}", travelServiceUrl);
            log.info(" Reviews Service:         {}", reviewsServiceUrl);
            log.info(" Blog Service:            {}", blogServiceUrl);
            log.info(" Customer Support:        {}", customerSupportServiceUrl);
            log.info(" Utils Service:           {}", utilsServiceUrl);
            log.info(" Core Service:            {}", coreServiceUrl);
            log.info(" Chat Service:            {}", chatServiceUrl);
            log.info("=".repeat(80));
            log.info(" CRM/ERP Services:");
            log.info("    Employee Service:      {}", employeeServiceUrl);
            log.info("    Agent Service:         {}", agentServiceUrl);
            log.info("    Lead Service:          {}", leadServiceUrl);
            log.info("    Supplier Service:      {}", supplierServiceUrl);
            log.info("    Wallet Service:         {}", walletServiceUrl);
            log.info("    Commission Service:    {}", commissionServiceUrl);
            log.info("    Settlement Service:    {}", supplierSettlementServiceUrl);
            log.info("    GST Service:           {}", gstServiceUrl);
            log.info("    Fraud Service:         {}", fraudServiceUrl);
            log.info("    Document Service:      {}", documentServiceUrl);
            log.info("    Audit Service:         {}", auditServiceUrl);
            log.info("    Report Service:        {}", reportServiceUrl);
            log.info("=".repeat(80));
            log.info(" Security: JWT Authentication + CORS enabled");
            log.info(" Rate Limiting: Redis-based (configurable)");
            log.info(" Circuit Breaker: Resilience4j enabled");
            log.info("=".repeat(80));
        }
    }
}