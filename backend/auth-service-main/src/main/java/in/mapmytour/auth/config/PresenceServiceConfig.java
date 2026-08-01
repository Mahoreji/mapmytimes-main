package in.mapmytour.auth.config;

import in.mapmytour.auth.service.PresenceService;
import in.mapmytour.auth.service.impl.PresenceServiceImpl;
import in.mapmytour.auth.service.impl.PresenceServiceRedisImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration to switch between in-memory and Redis-based presence service
 * 
 * Set presence.service.impl=redis to use Redis (for multi-instance)
 * Set presence.service.impl=memory to use in-memory (for single instance)
 */
@Configuration
public class PresenceServiceConfig {

    @Value("${presence.service.impl:memory}")
    private String presenceServiceImpl;

    private final in.mapmytour.auth.repository.UserRepository userRepository;
    private final in.mapmytour.auth.repository.UserConnectionRepository userConnectionRepository;
    @Lazy
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public PresenceServiceConfig(
            in.mapmytour.auth.repository.UserRepository userRepository,
            in.mapmytour.auth.repository.UserConnectionRepository userConnectionRepository,
            @Lazy org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.userConnectionRepository = userConnectionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "presence.service.impl", havingValue = "redis", matchIfMissing = false)
    @ConditionalOnBean(RedisTemplate.class)
    public PresenceService presenceServiceRedis() {
        if (redisTemplate == null) {
            throw new IllegalStateException("RedisTemplate bean is required for Redis presence service but not found. Please configure Redis or use presence.service.impl=memory");
        }
        return new PresenceServiceRedisImpl(
                userRepository,
                userConnectionRepository,
                messagingTemplate,
                redisTemplate
        );
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "presence.service.impl", havingValue = "memory", matchIfMissing = true)
    public PresenceService presenceServiceMemory() {
        return new PresenceServiceImpl(
                userRepository,
                userConnectionRepository,
                messagingTemplate
        );
    }
}

