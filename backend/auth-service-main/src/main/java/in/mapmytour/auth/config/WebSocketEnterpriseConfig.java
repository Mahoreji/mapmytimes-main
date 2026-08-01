package in.mapmytour.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Enterprise-grade WebSocket configuration with external message broker
 * Use this for production with multiple instances
 * 
 * To enable: Set spring.profiles.active=prod,enterprise
 * 
 * Requires:
 * - RabbitMQ or Redis configured as message broker
 * - Redis for presence tracking
 */
@Configuration
@EnableWebSocketMessageBroker
@Profile("enterprise")
public class WebSocketEnterpriseConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitmqPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitmqPassword;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use external message broker (RabbitMQ) for production
        // This enables message delivery across multiple server instances
        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(rabbitmqHost)
                .setRelayPort(rabbitmqPort)
                .setClientLogin(rabbitmqUsername)
                .setClientPasscode(rabbitmqPassword)
                .setSystemLogin(rabbitmqUsername)
                .setSystemPasscode(rabbitmqPassword);
        
        // Prefix for messages FROM client TO server
        config.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix - required for convertAndSendToUser to work
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /ws and /ws/websocket endpoints for WebSocket connections
        // Clients may append /websocket to the base endpoint, so we register both
        // SockJS will automatically create /ws/info and /ws/websocket/info endpoints
        // 
        // IMPORTANT: setAllowedOriginPatterns("*") allows all origins for WebSocket handshake
        // This is different from HTTP CORS headers - it's for WebSocket origin validation
        // The HTTP /ws/info endpoint gets CORS headers ONLY from API Gateway (no duplicates)
        // The WebSocket upgrade request uses this origin validation
        // 
        // When accessed through API Gateway (/auth/ws/info or /auth/ws/websocket/info):
        // - HTTP CORS headers: Set by API Gateway's CorsWebFilter (single source)
        // - WebSocket origin validation: Uses setAllowedOriginPatterns("*") here
        // This prevents duplicate HTTP CORS headers while allowing WebSocket connections
        registry.addEndpoint("/ws", "/ws/websocket", "/api/v1/auth/ws", "/api/v1/auth/ws/websocket")
                .setAllowedOriginPatterns(allowedOrigins) // Use specific origins from properties
                .withSockJS(); // Enable SockJS fallback options - creates /ws/info automatically
    }
}

