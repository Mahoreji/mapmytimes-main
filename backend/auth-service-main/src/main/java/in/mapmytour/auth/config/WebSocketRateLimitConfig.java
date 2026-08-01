package in.mapmytour.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting for WebSocket operations
 * Prevents abuse and DoS attacks
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketRateLimitConfig implements WebSocketMessageBrokerConfigurer {

    // Rate limits per user
    private static final int MAX_TYPING_INDICATORS_PER_MINUTE = 10;
    private static final int MAX_MESSAGES_PER_MINUTE = 60;
    private static final int MAX_HEARTBEATS_PER_MINUTE = 20;
    
    // Track rate limits per user
    private final ConcurrentHashMap<String, RateLimitTracker> rateLimitMap = new ConcurrentHashMap<>();
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    Principal principal = accessor.getUser();
                    if (principal != null) {
                        String userEmail = principal.getName();
                        String destination = accessor.getDestination();
                        
                        if (destination != null) {
                            // Rate limit typing indicators
                            if (destination.contains("/typing")) {
                                if (!checkRateLimit(userEmail, "typing", MAX_TYPING_INDICATORS_PER_MINUTE)) {
                                    log.warn("Rate limit exceeded for typing indicators: {}", userEmail);
                                    return null; // Drop message
                                }
                            }
                            
                            // Rate limit messages
                            if (destination.contains("/message")) {
                                if (!checkRateLimit(userEmail, "message", MAX_MESSAGES_PER_MINUTE)) {
                                    log.warn("Rate limit exceeded for messages: {}", userEmail);
                                    return null; // Drop message
                                }
                            }
                            
                            // Rate limit heartbeats
                            if (destination.contains("/heartbeat")) {
                                if (!checkRateLimit(userEmail, "heartbeat", MAX_HEARTBEATS_PER_MINUTE)) {
                                    // Heartbeat rate limit is less strict - just log
                                    log.debug("Heartbeat rate limit warning: {}", userEmail);
                                }
                            }
                        }
                    }
                }
                
                return message;
            }
        });
    }
    
    private boolean checkRateLimit(String userEmail, String type, int maxPerMinute) {
        String key = userEmail + ":" + type;
        RateLimitTracker tracker = rateLimitMap.computeIfAbsent(key, k -> new RateLimitTracker());
        return tracker.checkAndIncrement(maxPerMinute);
    }
    
    private static class RateLimitTracker {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private static final long WINDOW_MS = 60000; // 1 minute
        
        public boolean checkAndIncrement(int maxPerMinute) {
            long now = System.currentTimeMillis();
            long elapsed = now - windowStart;
            
            if (elapsed > WINDOW_MS) {
                // Reset window
                count.set(0);
                windowStart = now;
            }
            
            int current = count.incrementAndGet();
            return current <= maxPerMinute;
        }
    }
}

