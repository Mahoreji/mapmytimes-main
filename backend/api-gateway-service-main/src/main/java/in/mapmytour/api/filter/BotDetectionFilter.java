package in.mapmytour.api.filter;

import in.mapmytour.api.service.SecurityCacheService;
import in.mapmytour.api.service.SecurityLoggingService;
import in.mapmytour.api.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * BotDetectionFilter — Identifies and blocks automated scanning tools,
 * attack frameworks, and crawler bots before they reach backend services.
 *
 * Detection mechanisms:
 *   1. User-Agent signature matching (sqlmap, nikto, dirbuster, etc.)
 *   2. Requests to common scan targets (.env, .git, wp-admin, etc.)
 *   3. Sequential endpoint scanning (high-frequency 404s from a single IP)
 *
 * Detected bots are banned in Redis for a configurable cooldown period.
 *
 * Filter order: -150 — runs after AdminIpWhitelistFilter (-200) but
 * before ThreatScoring (-100).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BotDetectionFilter implements GlobalFilter, Ordered {

    private final SecurityCacheService securityCacheService;
    private final SecurityLoggingService loggingService;

    @Value("${security.bot.ban-duration-hours:24}")
    private long botBanDurationHours;

    @Value("${security.bot.scan-count-threshold:15}")
    private long scanCountThreshold;

    // ── Known malicious User-Agent signatures ─────────────────────────────
    private static final List<Pattern> MALICIOUS_UA_PATTERNS = List.of(
            Pattern.compile("(?i)sqlmap"),
            Pattern.compile("(?i)nikto"),
            Pattern.compile("(?i)dirbuster"),
            Pattern.compile("(?i)dirb[^a-z]"),
            Pattern.compile("(?i)masscan"),
            Pattern.compile("(?i)nmap"),
            Pattern.compile("(?i)burp\\s*intruder"),
            Pattern.compile("(?i)burpsuite"),
            Pattern.compile("(?i)owasp\\s*zap"),
            Pattern.compile("(?i)w3af"),
            Pattern.compile("(?i)acunetix"),
            Pattern.compile("(?i)scrapy"),
            Pattern.compile("(?i)libwww-perl"),
            Pattern.compile("(?i)nuclei")
    );

    // ── Common scan / admin path probing targets ──────────────────────────
    private static final List<String> SCAN_TARGET_PATHS = List.of(
            "/.env", "/.git", "/.svn", "/.hg",
            "/wp-admin", "/wp-login.php", "/xmlrpc.php",
            "/phpmyadmin", "/pma", "/myadmin",
            "/admin.php", "/admin/config", "/admin/login",
            "/config.php", "/configuration.php",
            "/backup", "/backup.zip", "/dump.sql",
            "/.ssh", "/.aws", "/.config",
            "/server-status", "/info.php", "/phpinfo.php",
            "/actuator/env", "/actuator/trace",
            "/console", "/manager", "/jmx-console"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 🛡️ Skip for whitelisted IPs
        if (Boolean.TRUE.equals(exchange.getAttribute(AdminIpWhitelistFilter.WHITELIST_ATTRIBUTE))) {
            return chain.filter(exchange);
        }

        String clientIp  = IpUtils.resolveClientIp(exchange.getRequest());
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String path      = exchange.getRequest().getURI().getPath();
        String method    = exchange.getRequest().getMethod().name();

        // Step 1: Check if IP is already in the ban cache
        return securityCacheService.isIpBanned(clientIp)
                .flatMap(banned -> {
                    if (banned) {
                        log.warn("Bot ban hit: ip={} path={}", clientIp, path);
                        return blockRequest(exchange, clientIp, path, method,
                                "IP is in bot ban cache", userAgent);
                    }

                    // Step 2: Check User-Agent signature
                    if (isMaliciousUserAgent(userAgent)) {
                        return securityCacheService.banIp(clientIp, Duration.ofHours(botBanDurationHours))
                                .then(blockRequest(exchange, clientIp, path, method,
                                        "Malicious User-Agent detected", userAgent));
                    }

                    // Step 3: Check if the path is a known scan target
                    if (isScanTarget(path)) {
                        return securityCacheService.incrementScanCount(clientIp)
                                .flatMap(count -> {
                                    if (count >= scanCountThreshold) {
                                        return securityCacheService.banIp(clientIp, Duration.ofHours(botBanDurationHours))
                                                .then(blockRequest(exchange, clientIp, path, method,
                                                        "Sequential scan detected (count=" + count + ")", userAgent));
                                    }
                                    // Not yet over threshold, but still a scan target — add threat score
                                    return securityCacheService.incrementThreatScore(clientIp, 10)
                                            .then(chain.filter(exchange));
                                });
                    }

                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    log.error("BotDetectionFilter error for ip={}: {}", clientIp, e.getMessage());
                    return chain.filter(exchange); // Fail-open on internal errors
                });
    }

    private boolean isMaliciousUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return false;
        return MALICIOUS_UA_PATTERNS.stream()
                .anyMatch(p -> p.matcher(userAgent).find());
    }

    private boolean isScanTarget(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return SCAN_TARGET_PATHS.stream().anyMatch(lower::startsWith);
    }

    private Mono<Void> blockRequest(ServerWebExchange exchange, String clientIp,
                                    String path, String method,
                                    String reason, String userAgent) {
        loggingService.logBotDetection(clientIp, path, method, userAgent != null ? userAgent : "none");
        log.warn("Bot/scanner blocked: ip={} path={} reason={}", clientIp, path, reason);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String body = "{\"success\":false,\"statusCode\":403,\"message\":\"Access Denied\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }


    @Override
    public int getOrder() {
        return -150;
    }
}
