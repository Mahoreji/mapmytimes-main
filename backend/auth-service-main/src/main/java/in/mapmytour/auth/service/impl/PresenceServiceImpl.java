package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.user.PresenceUpdateResponse;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.repository.UserConnectionRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service implementation for managing user presence
 * Note: Bean creation handled by PresenceServiceConfig
 * Remove @Service to avoid conflicts with conditional bean creation
 */
@RequiredArgsConstructor
@Slf4j
public class PresenceServiceImpl implements PresenceService {

    private final UserRepository userRepository;
    private final UserConnectionRepository userConnectionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // In-memory store of online users (use Redis in production for multi-instance)
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    
    // Track last activity time for each user
    private final ConcurrentHashMap<String, LocalDateTime> lastActivityMap = new ConcurrentHashMap<>();
    
    private static final int ONLINE_THRESHOLD_MINUTES = 5;

    @Override
    public void markUserOnline(String userEmail) {
        onlineUsers.add(userEmail);
        updateLastSeen(userEmail);
        broadcastPresenceUpdate(userEmail, createPresenceUpdate(userEmail, true));
        log.debug("User marked as online: {}", userEmail);
    }

    @Override
    public void markUserOffline(String userEmail) {
        onlineUsers.remove(userEmail);
        lastActivityMap.remove(userEmail);
        broadcastPresenceUpdate(userEmail, createPresenceUpdate(userEmail, false));
        log.debug("User marked as offline: {}", userEmail);
    }

    @Override
    public void updateLastSeen(String userEmail) {
        LocalDateTime now = LocalDateTime.now();
        lastActivityMap.put(userEmail, now);
        
        // Broadcast to connections if user is online
        if (isUserOnline(userEmail)) {
            broadcastPresenceUpdate(userEmail, createPresenceUpdate(userEmail, true));
        }
    }

    @Override
    public boolean isUserOnline(String userEmail) {
        if (!onlineUsers.contains(userEmail)) {
            return false;
        }
        
        // Check if user was active recently
        LocalDateTime lastActivity = lastActivityMap.get(userEmail);
        if (lastActivity == null) {
            return false;
        }
        
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(ONLINE_THRESHOLD_MINUTES);
        return lastActivity.isAfter(threshold);
    }

    @Override
    public java.util.Optional<LocalDateTime> getLastActivity(String userEmail) {
        return java.util.Optional.ofNullable(lastActivityMap.get(userEmail));
    }

    @Override
    public Set<String> getOnlineUsers() {
        // Clean up stale entries
        cleanupStaleUsers();
        return new HashSet<>(onlineUsers);
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
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(ONLINE_THRESHOLD_MINUTES);
        
        Set<String> staleUsers = new HashSet<>();
        for (String userEmail : onlineUsers) {
            LocalDateTime lastActivity = lastActivityMap.get(userEmail);
            if (lastActivity == null || lastActivity.isBefore(threshold)) {
                staleUsers.add(userEmail);
            }
        }
        
        for (String userEmail : staleUsers) {
            markUserOffline(userEmail);
        }
        
        if (!staleUsers.isEmpty()) {
            log.debug("Cleaned up {} stale online users", staleUsers.size());
        }
    }
}

