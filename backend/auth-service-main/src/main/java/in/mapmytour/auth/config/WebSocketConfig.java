package in.mapmytour.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry messages back to the client
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages FROM client TO server
        config.setApplicationDestinationPrefixes("/app");
        // User destination prefix - required for convertAndSendToUser to work
        // When client subscribes to /user/queue/messages, Spring maps it to /user/{username}/queue/messages
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

