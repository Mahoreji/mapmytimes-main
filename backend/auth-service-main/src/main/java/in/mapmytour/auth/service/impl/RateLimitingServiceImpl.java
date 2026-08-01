package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.service.RateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class RateLimitingServiceImpl implements RateLimitingService {

    private final Map<String, List<LocalDateTime>> attemptStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> blockedUsers = new ConcurrentHashMap<>();

    @Override
    public boolean isRateLimited(String key, int maxAttempts, Duration timeWindow) {
        cleanupOldAttempts(key, timeWindow);

        List<LocalDateTime> attempts = attemptStorage.getOrDefault(key, new CopyOnWriteArrayList<>());

        if (attempts.size() >= maxAttempts) {
            log.warn("Rate limit exceeded for key: {} (attempts: {}, max: {})", key, attempts.size(), maxAttempts);
            return true;
        }

        return false;
    }

    @Override
    public void recordAttempt(String key) {
        List<LocalDateTime> attempts = attemptStorage.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        attempts.add(LocalDateTime.now());

        log.debug("Recorded attempt for key: {} (total attempts: {})", key, attempts.size());
    }

    @Override
    public int getRemainingAttempts(String key, int maxAttempts, Duration timeWindow) {
        cleanupOldAttempts(key, timeWindow);

        List<LocalDateTime> attempts = attemptStorage.getOrDefault(key, new CopyOnWriteArrayList<>());
        return Math.max(0, maxAttempts - attempts.size());
    }

    @Override
    public void resetRateLimit(String key) {
        attemptStorage.remove(key);
        log.debug("Rate limit reset for key: {}", key);
    }

    @Override
    public boolean isBlocked(String identifier) {
        LocalDateTime blockedUntil = blockedUsers.get(identifier);
        if (blockedUntil == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(blockedUntil)) {
            blockedUsers.remove(identifier);
            return false;
        }

        return true;
    }

    @Override
    public void blockTemporarily(String identifier, Duration blockDuration) {
        LocalDateTime blockedUntil = LocalDateTime.now().plus(blockDuration);
        blockedUsers.put(identifier, blockedUntil);

        log.warn("User temporarily blocked: {} until {}", identifier, blockedUntil);
    }

    @Override
    public void removeBlock(String identifier) {
        blockedUsers.remove(identifier);
        log.info("Block removed for user: {}", identifier);
    }

    private void cleanupOldAttempts(String key, Duration timeWindow) {
        List<LocalDateTime> attempts = attemptStorage.get(key);
        if (attempts == null) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minus(timeWindow);
        attempts.removeIf(attempt -> attempt.isBefore(cutoff));

        if (attempts.isEmpty()) {
            attemptStorage.remove(key);
        }
    }

    /**
     * Scheduled cleanup method to remove old data
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredData() {
        // Cleanup old attempts
        attemptStorage.entrySet().removeIf(entry -> {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            entry.getValue().removeIf(attempt -> attempt.isBefore(oneHourAgo));
            return entry.getValue().isEmpty();
        });

        // Cleanup expired blocks
        LocalDateTime now = LocalDateTime.now();
        blockedUsers.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));

        log.debug("Cleaned up expired rate limiting data");
    }
}