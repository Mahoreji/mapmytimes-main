package in.mapmytour.auth.config;

import in.mapmytour.auth.service.PresenceService;
import in.mapmytour.auth.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PresenceService presenceService;

    public WebSocketSecurityConfig(UserDetailsService userDetailsService,
                                   JwtUtil jwtUtil,
                                   @Lazy PresenceService presenceService) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.presenceService = presenceService;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        // Extract JWT token from headers
                        String authToken = accessor.getFirstNativeHeader("Authorization");
                        
                        if (authToken != null && authToken.startsWith("Bearer ")) {
                            String token = authToken.substring(7);
                            
                            try {
                                // Validate token
                                if (jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token)) {
                                    // Extract user email from token (subject is the email)
                                    String userEmail = jwtUtil.getUsernameFromToken(token);

                                    if (userEmail != null) {
                                        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());
                                        accessor.setUser(auth);

                                        // Mark user as online and update last seen
                                        presenceService.markUserOnline(userEmail);

                                        log.debug("WebSocket authenticated user: {}", userEmail);
                                    }
                                } else {
                                    log.warn("Invalid or non-access token for WebSocket connection");
                                }
                            } catch (Exception e) {
                                log.error("WebSocket authentication error: {}", e.getMessage());
                            }
                        } else {
                            log.warn("No Authorization header found in WebSocket connection");
                        }
                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        // Handle disconnect - mark user as offline
                        Authentication auth = (Authentication) accessor.getUser();
                        if (auth != null && auth.getName() != null) {
                            String userEmail = auth.getName();
                            presenceService.markUserOffline(userEmail);
                            log.debug("WebSocket disconnected user: {}", userEmail);
                        }
                    }
                }
                
                return message;
            }
        });
    }
}

