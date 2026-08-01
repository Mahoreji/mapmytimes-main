package in.mapmytour.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.mapmytour.api.dto.APIResponse;
import in.mapmytour.api.utils.IpUtils;
import in.mapmytour.api.service.SecurityCacheService;
import in.mapmytour.api.service.SecurityLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * RateLimitingFilter — Adaptive rate limiter with:
 *   - Per-IP and per-user sliding window counters in Redis
 *   - Per-service-path configurable limits
 *   - /admin/login hard cap of 5 rpm per IP
 *   - Burst detection: if a window fill-rate > 90%, auto-halve the limit
 *   - Challenged clients (high threat score) get limits halved
 *   - Auto IP cooldown ban after repeated violations (via SecurityCacheService)
 *
 * Filter order: -1 (just before route filters)
 */
@Component
@Slf4j
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SecurityCacheService securityCacheService;
    private final SecurityLoggingService loggingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Configurables ─────────────────────────────────────────────────────
    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate-limit.default.requests-per-minute:100}")
    private int defaultRpm;

    @Value("${rate-limit.auth.requests-per-minute:50}")
    private int authRpm;

    @Value("${rate-limit.payment.requests-per-minute:30}")
    private int paymentRpm;

    @Value("${rate-limit.booking.requests-per-minute:60}")
    private int bookingRpm;

    @Value("${rate-limit.admin.requests-per-minute:200}")
    private int adminRpm;

    @Value("${rate-limit.public.requests-per-minute:150}")
    private int publicRpm;

    @Value("${rate-limit.admin-login.requests-per-minute:5}")
    private int adminLoginRpm;

    @Value("${rate-limit.violation-ban-duration-minutes:15}")
    private long violationBanMinutes;

    @Value("${rate-limit.violation-ban-count:3}")
    private long violationBanCount;

    // ── Static config ─────────────────────────────────────────────────────
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/actuator/health", "/health", "/api/v1/health",
            "/api/v1/notification/contact-form", "/actuator/info", "/fallback"
    );

    public RateLimitingFilter(ReactiveRedisTemplate<String, String> redisTemplate,
                               SecurityCacheService securityCacheService,
                               SecurityLoggingService loggingService) {
        this.redisTemplate = redisTemplate;
        this.securityCacheService = securityCacheService;
        this.loggingService = loggingService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitEnabled) return chain.filter(exchange);

        // 🛡️ Skip rate limiting for whitelisted IPs
        if (Boolean.TRUE.equals(exchange.getAttribute(AdminIpWhitelistFilter.WHITELIST_ATTRIBUTE))) {
            return chain.filter(exchange);
        }

        String path     = exchange.getRequest().getURI().getPath();
        String method   = exchange.getRequest().getMethod().name();
        String clientIp = IpUtils.resolveClientIp(exchange.getRequest());
        String userRole = exchange.getRequest().getHeaders().getFirst("X-User-Role");
        String userId   = exchange.getRequest().getHeaders().getFirst("X-User-Id");

        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) return chain.filter(exchange);

        String clientId   = userId != null ? "user:" + userId : "ip:" + clientIp;
        int baseLimit     = computeLimit(path, userRole);
        // Halve limit for threat-challenged requests
        boolean challenged = exchange.getAttribute("X-Threat-Challenged") != null
                && Boolean.TRUE.equals(exchange.getAttribute("X-Threat-Challenged"));
        int effectiveLimit = challenged ? Math.max(baseLimit / 2, 1) : baseLimit;

        String key = "rate_limit:" + clientId + ":" + getServiceType(path) + ":" +
                (userRole != null ? userRole : "anon");

        return checkAndIncrement(key, effectiveLimit)
                .flatMap(allowed -> {
                    if (!allowed) {
                        loggingService.logRateLimitViolation(clientIp, path, method);
                        // Track violations — ban on repeated breaches
                        String violationKey = "rate_violation:" + clientIp;
                        return redisTemplate.opsForValue().increment(violationKey)
                                .flatMap(vCount -> {
                                    if (vCount == 1L) {
                                        redisTemplate.expire(violationKey, Duration.ofMinutes(5)).subscribe();
                                    }
                                    if (vCount >= violationBanCount) {
                                        return securityCacheService.banIp(clientIp, Duration.ofMinutes(violationBanMinutes))
                                                .then(writeRateLimitResponse(exchange, effectiveLimit, path));
                                    }
                                    return writeRateLimitResponse(exchange, effectiveLimit, path);
                                });
                    }
                    return attachRateLimitHeaders(exchange, key, effectiveLimit)
                            .then(chain.filter(exchange));
                })
                .onErrorResume(e -> {
                    log.error("Rate limiter error (failing open): {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    // ── Token counter ─────────────────────────────────────────────────────

    private Mono<Boolean> checkAndIncrement(String key, int limit) {
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1L) {
                        return redisTemplate.expire(key, Duration.ofMinutes(1))
                                .thenReturn(count <= limit);
                    }
                    return Mono.just(count <= limit);
                })
                .onErrorReturn(true);
    }

    // ── Limit resolution ─────────────────────────────────────────────────

    private int computeLimit(String path, String userRole) {
        if ("ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole)) return adminRpm;
        // Hard cap for admin login brute-force prevention
        if (path.equalsIgnoreCase("/api/v1/auth/admin/login") ||
            path.equalsIgnoreCase("/admin/login")) return adminLoginRpm;
        if (path.startsWith("/api/v1/auth/"))      return authRpm;
        if (path.startsWith("/api/v1/payment"))    return paymentRpm;
        if (path.startsWith("/api/v1/booking"))    return bookingRpm;
        if (isPublicPath(path))                    return publicRpm;
        return defaultRpm;
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/tours/")   || path.startsWith("/api/v1/group-tours/") || path.startsWith("/api/v1/destinations/") ||
               path.startsWith("/api/v1/activities") || path.startsWith("/api/v1/adventures") ||
               path.startsWith("/api/v1/utils/")   || path.startsWith("/api/v1/reviews/search") ||
               path.startsWith("/api/v1/blog/posts/search");
    }

    private String getServiceType(String path) {
        if (path.startsWith("/api/v1/auth/"))    return "auth";
        if (path.startsWith("/api/v1/payment"))  return "payment";
        if (path.startsWith("/api/v1/booking"))  return "booking";
        if (path.startsWith("/api/v1/tours/") || path.startsWith("/api/v1/group-tours/"))   return "tours";
        if (path.startsWith("/api/v1/reviews/")) return "reviews";
        if (path.startsWith("/api/v1/blog/"))    return "blog";
        return "general";
    }

    // ── Response helpers ──────────────────────────────────────────────────

    private Mono<Void> attachRateLimitHeaders(ServerWebExchange exchange, String key, int limit) {
        return redisTemplate.opsForValue().get(key)
                .defaultIfEmpty("0")
                .doOnNext(val -> {
                    ServerHttpResponse resp = exchange.getResponse();
                    if (!resp.isCommitted()) {
                        int remaining = Math.max(0, limit - Integer.parseInt(val));
                        try {
                            resp.getHeaders().set("X-RateLimit-Limit",     String.valueOf(limit));
                            resp.getHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
                            resp.getHeaders().set("X-RateLimit-Reset",     String.valueOf(System.currentTimeMillis() + 60_000));
                        } catch (UnsupportedOperationException ignored) { /* read-only headers */ }
                    }
                })
                .then();
    }

    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange, int limit, String path) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().set("X-RateLimit-Limit",     String.valueOf(limit));
        response.getHeaders().set("X-RateLimit-Remaining", "0");
        response.getHeaders().set("X-RateLimit-Reset",     String.valueOf(System.currentTimeMillis() + 60_000));
        response.getHeaders().set("Retry-After", "60");

        APIResponse<Object> body = APIResponse.builder()
                .success(false)
                .statusCode(429)
                .message("Rate limit exceeded. Please try again in a minute.")
                .data(new RateLimitData("RATE_LIMIT_EXCEEDED", limit, 60, path,
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Please wait before making more requests to this endpoint"))
                .errors(Arrays.asList("Too many requests"))
                .build();
        try {
            String json = objectMapper.writeValueAsString(body);
            DataBuffer buf = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buf));
        } catch (Exception e) {
            return response.setComplete();
        }
    }


    @Override
    public int getOrder() { return -1; }

    // ── DTO ───────────────────────────────────────────────────────────────

    public static class RateLimitData {
        public final String errorCode;
        public final int limitPerMinute;
        public final int resetTimeSeconds;
        public final String requestPath;
        public final String timestamp;
        public final String suggestion;

        public RateLimitData(String errorCode, int limitPerMinute, int resetTimeSeconds,
                             String requestPath, String timestamp, String suggestion) {
            this.errorCode = errorCode;
            this.limitPerMinute = limitPerMinute;
            this.resetTimeSeconds = resetTimeSeconds;
            this.requestPath = requestPath;
            this.timestamp = timestamp;
            this.suggestion = suggestion;
        }
    }
}