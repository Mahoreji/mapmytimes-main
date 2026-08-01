package in.mapmytour.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connection limits per user to prevent resource exhaustion
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectionLimitConfig implements WebSocketMessageBrokerConfigurer {

    private static final int MAX_CONNECTIONS_PER_USER = 5;
    private final ConcurrentHashMap<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        Principal principal = accessor.getUser();
                        if (principal != null) {
                            String userEmail = principal.getName();
                            
                            AtomicInteger count = connectionCounts.computeIfAbsent(userEmail, k -> new AtomicInteger(0));
                            int currentConnections = count.incrementAndGet();
                            
                            if (currentConnections > MAX_CONNECTIONS_PER_USER) {
                                log.warn("Connection limit exceeded for user: {} ({} connections)", 
                                        userEmail, currentConnections);
                                count.decrementAndGet();
                                return null; // Reject connection
                            }
                            
                            log.debug("User {} connected (total connections: {})", userEmail, currentConnections);
                        }
                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        Principal principal = accessor.getUser();
                        if (principal != null) {
                            String userEmail = principal.getName();
                            AtomicInteger count = connectionCounts.get(userEmail);
                            if (count != null) {
                                int remaining = count.decrementAndGet();
                                if (remaining <= 0) {
                                    connectionCounts.remove(userEmail);
                                }
                                log.debug("User {} disconnected (remaining connections: {})", userEmail, remaining);
                            }
                        }
                    }
                }
                
                return message;
            }
        });
    }
}

