package in.mapmytour.api.service;

import in.mapmytour.api.entity.IpWhitelistEntry;
import in.mapmytour.api.repository.IpWhitelistRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Redis-backed security cache with PostgreSQL persistence.
 *
 * Manages:
 *   - Admin IP whitelist (Synced with DB)
 *   - Bot IP bans (Redis only)
 *   - Per-IP threat score accumulation (Redis only)
 *   - Auth failure counting (for brute-force scoring)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityCacheService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final IpWhitelistRepository ipWhitelistRepository;

    // ── Key Prefixes ────────────────────────────────────────────────────────
    private static final String IP_WHITELIST_PREFIX  = "sec:admin_whitelist:";
    private static final String THREAT_SCORE_PREFIX  = "sec:threat_score:";
    private static final String BOT_BAN_PREFIX       = "sec:bot_ban:";
    private static final String AUTH_FAIL_PREFIX     = "sec:auth_fail:";
    private static final String SCAN_COUNT_PREFIX    = "sec:scan_count:";

    // ── Default TTLs ────────────────────────────────────────────────────────
    public static final Duration DEFAULT_WHITELIST_TTL  = Duration.ofDays(30);
    public static final Duration PERMANENT_TTL          = Duration.ofDays(365 * 100); // 100 years = Effectively Permanent
    public static final Duration DEFAULT_BOT_BAN_TTL    = Duration.ofHours(24);
    public static final Duration THREAT_SCORE_WINDOW    = Duration.ofMinutes(5);
    public static final Duration AUTH_FAIL_WINDOW       = Duration.ofMinutes(5);

    /**
     * Production Health Check: Verifies if Redis is reachable.
     */
    public Mono<Boolean> isRedisHealthy() {
        return redisTemplate.execute(conn -> conn.ping())
                .next()
                .map(res -> "PONG".equalsIgnoreCase(res))
                .onErrorReturn(false)
                .defaultIfEmpty(false);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Initialization: Bootstrap Redis from DB with Fail-Safe
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @PostConstruct
    public void bootstrapWhitelist() {
        log.info("Production Security: Bootstrapping IP whitelist from PostgreSQL to Redis...");
        
        ipWhitelistRepository.findAllByActive(true)
                .filter(entry -> entry.getExpiresAt() == null || entry.getExpiresAt().isAfter(LocalDateTime.now()))
                .flatMap(entry -> {
                    String key = IP_WHITELIST_PREFIX + sanitize(entry.getIpAddress());
                    if (entry.getExpiresAt() == null) {
                        return redisTemplate.opsForValue().set(key, "APPROVED");
                    } else {
                        long seconds = Duration.between(LocalDateTime.now(), entry.getExpiresAt()).getSeconds();
                        return redisTemplate.opsForValue().set(key, "APPROVED", Duration.ofSeconds(Math.max(seconds, 1)));
                    }
                })
                .doOnError(e -> log.error("Production Security: Failed to load whitelisted IPs from DB! Gateway will run in cache-only mode.", e))
                .collectList()
                .subscribe(results -> log.info("Production Security: Successfully synchronized {} active IPs to cache", results.size()));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Admin IP Whitelist
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Add an IP to the admin whitelist with a specific TTL.
     * Persists to DB and syncs to Redis.
     */
    public Mono<Boolean> whitelistIp(String ip, Duration ttl, String addedBy, String label) {
        String key = IP_WHITELIST_PREFIX + sanitize(ip);
        LocalDateTime expiresAt = ttl != null ? LocalDateTime.now().plus(ttl) : null;

        log.info("Admin whitelist: Attempting to whitelist IP {} (TTL: {})", ip, ttl != null ? ttl : "PERMANENT");

        return ipWhitelistRepository.findByIpAddress(ip)
                .flatMap(existing -> {
                    existing.setLabel(label);
                    existing.setAddedBy(addedBy);
                    existing.setExpiresAt(expiresAt);
                    existing.setActive(true);
                    existing.setNew(false);
                    return ipWhitelistRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    IpWhitelistEntry newEntry = IpWhitelistEntry.builder()
                            .id(UUID.randomUUID().toString())
                            .ipAddress(ip)
                            .label(label)
                            .addedBy(addedBy)
                            .expiresAt(expiresAt)
                            .active(true)
                            .isNew(true)
                            .build();
                    return ipWhitelistRepository.save(newEntry);
                }))
                .flatMap(e -> {
                    if (ttl != null) {
                        return redisTemplate.opsForValue().set(key, "APPROVED", ttl);
                    } else {
                        // For permanent entries, use an effectively infinite TTL to stay HOT in cache
                        return redisTemplate.opsForValue().set(key, "APPROVED", PERMANENT_TTL);
                    }
                })
                .doOnSuccess(r -> log.info("Admin whitelist: Successfully persisted and cached IP {}", ip))
                .doOnError(e -> log.error("Admin whitelist: Failed to whitelist IP {} - Error: {}", ip, e.getMessage()));
    }

    /**
     * Add an IP to the admin whitelist with a specific TTL.
     */
    public Mono<Boolean> whitelistIp(String ip, Duration ttl) {
        return whitelistIp(ip, ttl, "SYSTEM", "Automated Whitelist");
    }

    /**
     * Add an IP to the admin whitelist with the default TTL.
     */
    public Mono<Boolean> whitelistIp(String ip) {
        return whitelistIp(ip, DEFAULT_WHITELIST_TTL);
    }

    /**
     * Get all currently whitelisted IP addresses.
     */
    public reactor.core.publisher.Flux<IpWhitelistEntry> getAllWhitelistedIps() {
        return ipWhitelistRepository.findAllByActive(true)
                .filter(entry -> entry.getExpiresAt() == null || entry.getExpiresAt().isAfter(java.time.LocalDateTime.now()));
    }

    /**
     * Remove an IP from the admin whitelist immediately.
     */
    public Mono<Void> removeIpFromWhitelist(String ip) {
        return ipWhitelistRepository.deleteByIpAddress(ip)
                .then(redisTemplate.opsForValue().delete(IP_WHITELIST_PREFIX + sanitize(ip)))
                .doOnSuccess(r -> log.info("Admin whitelist: Removed and cleared cache for {}", ip))
                .then();
    }

    /**
     * Check whether an IP is currently on the admin whitelist.
     */
    public Mono<Boolean> isIpWhitelisted(String ip) {
        String key = IP_WHITELIST_PREFIX + sanitize(ip);
        return redisTemplate.hasKey(key)
                .flatMap(hasKey -> {
                    if (Boolean.TRUE.equals(hasKey)) {
                        return Mono.just(true);
                    }
                    return checkDbAndReCache(ip, key);
                })
                .onErrorResume(e -> {
                    log.warn("Redis error while checking whitelist for IP {}. Falling back to PostgreSQL DB. Error: {}", ip, e.getMessage());
                    return ipWhitelistRepository.findActiveByIp(ip, LocalDateTime.now())
                            .map(entry -> entry != null)
                            .defaultIfEmpty(false);
                });
    }

    private Mono<Boolean> checkDbAndReCache(String ip, String key) {
        return ipWhitelistRepository.findActiveByIp(ip, LocalDateTime.now())
                .flatMap(entry -> {
                    if (entry.getExpiresAt() == null) {
                        return redisTemplate.opsForValue().set(key, "APPROVED", PERMANENT_TTL)
                                .thenReturn(true)
                                .onErrorReturn(true);
                    } else {
                        long seconds = Duration.between(LocalDateTime.now(), entry.getExpiresAt()).getSeconds();
                        if (seconds > 0) {
                            return redisTemplate.opsForValue().set(key, "APPROVED", Duration.ofSeconds(Math.max(seconds, 1)))
                                    .thenReturn(true)
                                    .onErrorReturn(true);
                        }
                        return Mono.just(false);
                    }
                })
                .defaultIfEmpty(false);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Bot Ban Cache
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Ban an IP (e.g., detected bot / scanner) for a given cooldown window.
     */
    public Mono<Boolean> banIp(String ip, Duration duration) {
        String key = BOT_BAN_PREFIX + sanitize(ip);
        return redisTemplate.opsForValue()
                .set(key, "BANNED", duration)
                .doOnSuccess(r -> log.warn("Bot ban: {} banned for {}", ip, duration));
    }

    /**
     * Ban an IP with the default bot ban TTL.
     */
    public Mono<Boolean> banIp(String ip) {
        return banIp(ip, DEFAULT_BOT_BAN_TTL);
    }

    /**
     * Check if an IP is currently in the bot ban cache.
     */
    public Mono<Boolean> isIpBanned(String ip) {
        return redisTemplate.hasKey(BOT_BAN_PREFIX + sanitize(ip));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Threat Scoring
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Add points to an IP's rolling threat score.
     * Returns the new cumulative score.
     */
    public Mono<Integer> incrementThreatScore(String ip, int delta) {
        String key = THREAT_SCORE_PREFIX + sanitize(ip);
        return redisTemplate.opsForValue()
                .increment(key, delta)
                .flatMap(score -> {
                    if (score.equals((long) delta)) {
                        // First increment – set the rolling TTL window
                        return redisTemplate.expire(key, THREAT_SCORE_WINDOW)
                                .thenReturn(score.intValue());
                    }
                    return Mono.just(score.intValue());
                })
                .doOnNext(s -> log.debug("Threat score for {}: {} (delta +{})", ip, s, delta));
    }

    /**
     * Get the current threat score for an IP (0 if no data).
     */
    public Mono<Integer> getThreatScore(String ip) {
        return redisTemplate.opsForValue()
                .get(THREAT_SCORE_PREFIX + sanitize(ip))
                .map(val -> toInt(val, 0))
                .defaultIfEmpty(0);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Auth Failure Counting (for brute-force scoring)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Increment auth failure counter for an IP. Returns new count.
     */
    public Mono<Long> incrementAuthFailures(String ip) {
        String key = AUTH_FAIL_PREFIX + sanitize(ip);
        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1L) {
                        return redisTemplate.expire(key, AUTH_FAIL_WINDOW).thenReturn(count);
                    }
                    return Mono.just(count);
                });
    }

    /**
     * Get current auth failure count for an IP.
     */
    public Mono<Long> getAuthFailures(String ip) {
        return redisTemplate.opsForValue()
                .get(AUTH_FAIL_PREFIX + sanitize(ip))
                .map(val -> toLong(val, 0L))
                .defaultIfEmpty(0L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Sequential Scan Counting (for bot/scanner detection)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Track how many 404/scan requests an IP has made in the last window.
     */
    public Mono<Long> incrementScanCount(String ip) {
        String key = SCAN_COUNT_PREFIX + sanitize(ip);
        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1L) {
                        return redisTemplate.expire(key, Duration.ofMinutes(2)).thenReturn(count);
                    }
                    return Mono.just(count);
                });
    }

    /**
     * Get current scan count for an IP.
     */
    public Mono<Long> getScanCount(String ip) {
        return redisTemplate.opsForValue()
                .get(SCAN_COUNT_PREFIX + sanitize(ip))
                .map(val -> toLong(val, 0L))
                .defaultIfEmpty(0L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Utility
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** Strip characters that could pollute Redis key names. */
    /**
     * Get all currently tracked behavioral threat scores from Redis.
     */
    public reactor.core.publisher.Mono<java.util.Map<String, Integer>> getAllThreatScores() {
        return redisTemplate.keys(THREAT_SCORE_PREFIX + "*")
                .flatMap(key -> redisTemplate.opsForValue().get(key)
                        .map(score -> java.util.Map.entry(key.replace(THREAT_SCORE_PREFIX, ""), toInt(score, 0))))
                .collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue);
    }

    private int toInt(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value in Redis: {}", value);
            return defaultValue;
        }
    }

    private long toLong(String value, long defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid long value in Redis: {}", value);
            return defaultValue;
        }
    }

    private String sanitize(String ip) {
        return ip == null ? "unknown" : ip.replaceAll("[^0-9a-fA-F.:]", "");
    }
}
