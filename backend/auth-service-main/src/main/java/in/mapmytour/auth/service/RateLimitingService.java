package in.mapmytour.auth.service;

import java.time.Duration;

public interface RateLimitingService {

    /**
     * Check if an operation is rate limited for a given key
     */
    boolean isRateLimited(String key, int maxAttempts, Duration timeWindow);

    /**
     * Record an attempt for a given key
     */
    void recordAttempt(String key);

    /**
     * Get remaining attempts for a given key
     */
    int getRemainingAttempts(String key, int maxAttempts, Duration timeWindow);

    /**
     * Reset rate limit for a given key
     */
    void resetRateLimit(String key);

    /**
     * Check if user is temporarily blocked
     */
    boolean isBlocked(String identifier);

    /**
     * Block user temporarily
     */
    void blockTemporarily(String identifier, Duration blockDuration);

    /**
     * Remove temporary block
     */
    void removeBlock(String identifier);
}