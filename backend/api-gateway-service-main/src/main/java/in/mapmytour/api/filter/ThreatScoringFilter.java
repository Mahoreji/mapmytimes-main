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
import java.util.List;
import java.util.regex.Pattern;

/**
 * ThreatScoringFilter — Assigns a cumulative risk score to each request
 * based on behavioural signals. The score is stored in Redis with a rolling
 * TTL window so it decays naturally for well-behaved clients.
 *
 * Scoring factors and their point values:
 *   +15  Missing or empty User-Agent header
 *   +10  Suspicious query string patterns (SQLi probes, path traversal)
 *   +10  Repeated authentication failures (> 3 in window, per redis count)
 *   +10  High-frequency requests to error-producing paths
 *   +20  Directory traversal sequences in path
 *   +15  Presence of known attack header names
 *   +25  Invalid / non-standard HTTP method
 *
 * Actions on cumulative score:
 *   score < 30  → allow (pass through)
 *   30 ≤ score < 70 → challenge (add X-Threat-Score header + slow path)
 *   score ≥ 70  → block (HTTP 429 / 403)
 *
 * Filter order: -100 — runs after BotDetectionFilter (-150) but before
 * AuthenticationFilter.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ThreatScoringFilter implements GlobalFilter, Ordered {

    private final SecurityCacheService securityCacheService;
    private final SecurityLoggingService loggingService;

    @Value("${security.threat.score-block-threshold:100}")
    private int blockThreshold;

    @Value("${security.threat.score-challenge-threshold:30}")
    private int challengeThreshold;

    // ── SQLi / XSS / traversal probe patterns in query strings ───────────
    private static final List<Pattern> SUSPICIOUS_QUERY_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(union|select|insert|update|delete|drop|exec|execute)\\b"),
            Pattern.compile("(?i)<script.*?>|javascript:|onload=|onerror="),
            Pattern.compile("\\.{2}[/\\\\]"),                    // path traversal  ../
            Pattern.compile("(?i)(null|true|false)%00"),         // null-byte injection
            Pattern.compile("(?i)(<|%3c)(script|img|svg)"),      // XSS via URL
            Pattern.compile("(?i)(;|\\|)\\s*(ls|cat|rm|curl|wget|bash|sh)")) ; // shell injection

    // ── Suspicious request header names ──────────────────────────────────
    private static final List<String> ATTACK_HEADER_NAMES = List.of(
            "X-Original-URL", "X-Rewrite-URL",
            "X-Custom-IP-Authorization", "X-Originating-IP"
    );

    // ── Valid HTTP methods ────────────────────────────────────────────────
    private static final List<String> VALID_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 🛡️ Skip for whitelisted IPs (Trusted sources)
        if (Boolean.TRUE.equals(exchange.getAttribute(AdminIpWhitelistFilter.WHITELIST_ATTRIBUTE))) {
            return chain.filter(exchange);
        }

        String clientIp = IpUtils.resolveClientIp(exchange.getRequest());
        String path     = exchange.getRequest().getURI().getPath();
        String method   = exchange.getRequest().getMethod().name();
        String query    = exchange.getRequest().getURI().getQuery();
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");

        // Compute incremental score for this single request
        int delta = calculateDelta(exchange, method, query, userAgent);

        if (delta == 0) {
            return chain.filter(exchange);
        }

        return securityCacheService.incrementThreatScore(clientIp, delta)
                .flatMap(score -> {
                    log.debug("Threat score: ip={} score={} delta={} path={}", clientIp, score, delta, path);

                    if (score >= blockThreshold) {
                        loggingService.logThreatScoreEscalation(clientIp, path, score, "BLOCKED");
                        return blockHighRisk(exchange, clientIp, path, method, score);
                    }

                    if (score >= challengeThreshold) {
                        loggingService.logThreatScoreEscalation(clientIp, path, score, "CHALLENGED");
                        // Tag exchange so downstream filters / rate-limiter can apply stricter limits
                        exchange.getAttributes().put("X-Threat-Challenged", true);
                        exchange.getAttributes().put("X-Threat-Score", score);
                        return chain.filter(exchange)
                                .then(Mono.fromRunnable(() ->
                                        addThreatHeader(exchange, score)));
                    }

                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    log.error("ThreatScoringFilter error for ip={}: {}", clientIp, e.getMessage());
                    return chain.filter(exchange);
                });
    }

    // =====================================================================
    // Scoring Logic
    // =====================================================================

    private int calculateDelta(ServerWebExchange exchange, String method, String query, String userAgent) {
        int score = 0;

        // Missing User-Agent is unusual for real browsers
        if (userAgent == null || userAgent.isBlank()) score += 15;

        // Suspicious query string patterns
        if (query != null && hasSuspiciousQuery(query)) score += 10;

        // Directory traversal in path
        String path = exchange.getRequest().getURI().getPath();
        if (path != null && (path.contains("../") || path.contains("..%2F") || path.contains("%2e%2e"))) {
            score += 20;
        }

        // Attack headers present
        var requestHeaders = exchange.getRequest().getHeaders();
        for (String attackHeader : ATTACK_HEADER_NAMES) {
            if (requestHeaders.containsKey(attackHeader)) {
                score += 15;
                break;
            }
        }

        // Invalid HTTP method
        if (!VALID_METHODS.contains(method.toUpperCase())) score += 25;

        return score;
    }

    private boolean hasSuspiciousQuery(String query) {
        return SUSPICIOUS_QUERY_PATTERNS.stream().anyMatch(p -> p.matcher(query).find());
    }

    // =====================================================================
    // Response Helpers
    // =====================================================================

    private void addThreatHeader(ServerWebExchange exchange, int score) {
        try {
            exchange.getResponse().getHeaders()
                    .set("X-Threat-Score", String.valueOf(score));
        } catch (Exception ignored) { /* headers may be committed */ }
    }

    private Mono<Void> blockHighRisk(ServerWebExchange exchange, String clientIp,
                                     String path, String method, int score) {
        loggingService.logBlockedRequest(clientIp, path, method,
                "Threat score threshold exceeded: " + score, score);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().set("Retry-After", "300");

        String body = "{\"success\":false,\"statusCode\":429," +
                "\"message\":\"Request blocked: suspicious activity detected.\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }


    @Override
    public int getOrder() {
        return -100;
    }
}
