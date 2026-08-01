package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.user.PresenceUpdateResponse;

import java.util.Set;

/**
 * Service for managing user presence (online/offline status)
 */
public interface PresenceService {
    
    /**
     * Mark user as online
     */
    void markUserOnline(String userEmail);
    
    /**
     * Mark user as offline
     */
    void markUserOffline(String userEmail);
    
    /**
     * Update user's last seen timestamp
     */
    void updateLastSeen(String userEmail);
    
    /**
     * Check if user is online
     */
    boolean isUserOnline(String userEmail);
    
    /**
     * Get all online users
     */
    Set<String> getOnlineUsers();
    
    /**
     * Get user's last activity timestamp from real-time storage
     */
    java.util.Optional<java.time.LocalDateTime> getLastActivity(String userEmail);

    /**
     * Broadcast presence update to user's connections
     */
    void broadcastPresenceUpdate(String userEmail, PresenceUpdateResponse update);
}

