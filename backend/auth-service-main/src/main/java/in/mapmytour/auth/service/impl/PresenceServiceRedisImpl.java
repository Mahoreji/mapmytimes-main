package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.user.PresenceUpdateResponse;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.repository.UserConnectionRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise-grade Presence Service using Redis for distributed presence tracking
 * Supports multiple server instances and horizontal scaling
 * Note: Bean creation handled by PresenceServiceConfig
 */
@RequiredArgsConstructor
@Slf4j
public class PresenceServiceRedisImpl implements PresenceService {

    private final UserRepository userRepository;
    private final UserConnectionRepository userConnectionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String ONLINE_USERS_KEY = "presence:online";
    private static final String LAST_ACTIVITY_KEY_PREFIX = "presence:activity:";
    private static final int ONLINE_THRESHOLD_MINUTES = 5;
    private static final int PRESENCE_TTL_SECONDS = 600; // 10 minutes TTL

    @Override
    public void markUserOnline(String userEmail) {
        try {
            // Add to online set in Redis
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userEmail);
            redisTemplate.expire(ONLINE_USERS_KEY, PRESENCE_TTL_SECONDS, TimeUnit.SECONDS);
            
            updateLastSeen(userEmail);
            broadcastPresenceUpdate(userEmail, createPresenceUpdate(userEmail, true));
            log.debug("User marked as online: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to mark user online in Redis: {}", e.getMessage(), e);
            // Fallback: still update database
            updateLastSeen(userEmail);
        }
    }

    @Override
    public void markUserOffline(String userEmail) {
        try {
            // Remove from online set
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userEmail);
            redisTemplate.delete(LAST_ACTIVITY_KEY_PREFIX + userEmail);
            
            broadcastPresenceUpdate(userEmail, createPresenceUpdate(userEmail, false));
            log.debug("User marked as offline: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to mark user offline in Redis: {}", e.getMessage(), e);
        }
    }

    @Override
    public void updateLastSeen(String userEmail) {
        try {
            LocalDateTime now = LocalDateTime.now();
            long timestamp = now.toEpochSecond(ZoneOffset.UTC);
            
            // Store in Redis with TTL
            String key = LAST_ACTIVITY_KEY_PREFIX + userEmail;
            redisTemplate.opsForValue().set(key, timestamp, PRESENCE_TTL_SECONDS, TimeUnit.SECONDS);
            
            // Database persistence is handled by UserServiceImpl.touchLastSeen
            // which calls this method after potentially updating the DB.
            
            // Broadcast to connections if user is online
            if (isUserOnline(userEmail)) {
                broadcastPresenceUpdate(userEmail, createPresenceUpdate(userEmail, true));
            }
        } catch (Exception e) {
            log.error("Failed to update last seen: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isUserOnline(String userEmail) {
        try {
            if (!Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userEmail))) {
                return false;
            }
            
            // Check if user was active recently
            String key = LAST_ACTIVITY_KEY_PREFIX + userEmail;
            Long timestamp = (Long) redisTemplate.opsForValue().get(key);
            
            if (timestamp == null) {
                return false;
            }
            
            LocalDateTime lastActivity = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(ONLINE_THRESHOLD_MINUTES);
            return lastActivity.isAfter(threshold);
        } catch (Exception e) {
            log.error("Failed to check online status: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public java.util.Optional<LocalDateTime> getLastActivity(String userEmail) {
        try {
            String key = LAST_ACTIVITY_KEY_PREFIX + userEmail;
            Object val = redisTemplate.opsForValue().get(key);
            if (val == null) return java.util.Optional.empty();
            
            long timestamp;
            if (val instanceof Integer) timestamp = ((Integer) val).longValue();
            else if (val instanceof Long) timestamp = (Long) val;
            else timestamp = Long.parseLong(val.toString());
            
            return java.util.Optional.of(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
        } catch (Exception e) {
            log.error("Failed to get last activity from Redis for {}: {}", userEmail, e.getMessage());
            return java.util.Optional.empty();
        }
    }

    @Override
    public Set<String> getOnlineUsers() {
        try {
            cleanupStaleUsers();
            Set<Object> members = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            Set<String> onlineUsers = new HashSet<>();
            if (members != null) {
                for (Object member : members) {
                    if (member instanceof String) {
                        onlineUsers.add((String) member);
                    }
                }
            }
            return onlineUsers;
        } catch (Exception e) {
            log.error("Failed to get online users: {}", e.getMessage(), e);
            return new HashSet<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void broadcastPresenceUpdate(String userEmail, PresenceUpdateResponse update) {
        if (update == null) return;
        
        try {
            // Use optimized query to get only emails as strings
            // This avoids LazyInitializationException and proxy issues
            Set<String> connectionEmails = new HashSet<>();
            
            // Get emails of users connected TO this user
            connectionEmails.addAll(userConnectionRepository.findAllConnectedEmailsByUserEmail(userEmail));
            
            // Get emails of users who HAVE this user as a connection
            connectionEmails.addAll(userConnectionRepository.findAllUserEmailsByConnectedUserEmail(userEmail));
            
            // Broadcast to all active connections
            for (String connectionEmail : connectionEmails) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            connectionEmail,
                            "/queue/presence",
                            update
                    );
                } catch (Exception e) {
                    log.warn("Failed to send presence update to {}: {}", connectionEmail, e.getMessage());
                }
            }
            
            log.debug("Broadcasted presence update for {} to {} connections", userEmail, connectionEmails.size());
        } catch (Exception e) {
            log.warn("Failed to broadcast presence update for {}: {}", userEmail, e.getMessage());
        }
    }

    private PresenceUpdateResponse createPresenceUpdate(String userEmail, boolean isOnline) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return null;
        }
        
        return PresenceUpdateResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .isOnline(isOnline)
                .lastSeenAt(user.getLastSeenAt())
                .status(isOnline ? "ONLINE" : "OFFLINE")
                .build();
    }

    /**
     * Clean up stale online users (not active for threshold time)
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupStaleUsers() {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            if (members == null || members.isEmpty()) {
                return;
            }
            
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(ONLINE_THRESHOLD_MINUTES);
            long thresholdTimestamp = threshold.toEpochSecond(ZoneOffset.UTC);
            
            Set<String> staleUsers = new HashSet<>();
            for (Object member : members) {
                if (member instanceof String) {
                    String userEmail = (String) member;
                    String key = LAST_ACTIVITY_KEY_PREFIX + userEmail;
                    Long timestamp = (Long) redisTemplate.opsForValue().get(key);
                    
                    if (timestamp == null || timestamp < thresholdTimestamp) {
                        staleUsers.add(userEmail);
                    }
                }
            }
            
            for (String userEmail : staleUsers) {
                markUserOffline(userEmail);
            }
            
            if (!staleUsers.isEmpty()) {
                log.debug("Cleaned up {} stale online users", staleUsers.size());
            }
        } catch (Exception e) {
            log.error("Failed to cleanup stale users: {}", e.getMessage(), e);
        }
    }
}

