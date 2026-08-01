package in.mapmytour.api.filter;

import in.mapmytour.api.service.SecurityCacheService;
import in.mapmytour.api.service.SecurityLoggingService;
import lombok.RequiredArgsConstructor;
import in.mapmytour.api.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * AdminIpWhitelistFilter — GlobalFilter that protects all /admin/** routes
 * by verifying the caller's IP against a Redis-backed whitelist.
 *
 * Only IPs explicitly added by an administrator via the SecurityAdminController
 * are permitted access. All other IPs receive a clean HTTP 404 response
 * (to hide the existence of the endpoint).
 *
 * Filter order: -200 — runs before BotDetection and ThreatScoring so
 * un-whitelisted IPs are dropped at the very beginning of the pipeline.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdminIpWhitelistFilter implements GlobalFilter, Ordered {

    private final SecurityCacheService securityCacheService;
    private final SecurityLoggingService loggingService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @org.springframework.beans.factory.annotation.Value("${security.admin.ip-whitelist.enabled:true}")
    private boolean whitelistEnabled;

    @org.springframework.beans.factory.annotation.Value("${security.admin.ip-whitelist.allow-private-networks:false}")
    private boolean allowPrivateNetworks;

    // ── Admin path patterns that require IP whitelisting ──────────────────
    private static final List<String> PROTECTED_PATTERNS = List.of(
            "/**/admin/**",
            "/**/super-admin/**",
            "/secure-gateway-admin/**"
    );

    // ── Publicly accessible admin helpers (Self-bootstrapping) ────────────
    private static final List<String> EXCLUDED_PATTERNS = List.of(
            "/api/v1/admin/security/whitelist-my-ip",
            "/api/v1/admin/security/whitelist/me",
            "/api/v1/admin/security/telemetry/**",
            "/actuator/**"
    );

    public static final String WHITELIST_ATTRIBUTE = "X-IP-Whitelisted";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = IpUtils.resolveClientIp(exchange.getRequest());
        String path     = exchange.getRequest().getURI().getPath();
        String method   = exchange.getRequest().getMethod().name();
        String userId   = exchange.getRequest().getHeaders().getFirst("X-User-Id");

        // 1. Check if the whitelist filter itself is disabled
        if (!whitelistEnabled) {
            if (isAdminPath(path) && !isExcluded(path)) {
                log.debug("Admin IP whitelist is DISABLED. Bypassing check for path={}", path);
            }
            exchange.getAttributes().put(WHITELIST_ATTRIBUTE, true);
            return chain.filter(exchange);
        }

        // 🛡️ Global Whitelist Check (Fastpath)
        // Check for localhost / Loopback to avoid lockouts
        if (IpUtils.isLocalhost(clientIp)) {
            exchange.getAttributes().put(WHITELIST_ATTRIBUTE, true);
            if (isAdminPath(path) && !isExcluded(path)) {
                log.info("Admin access GRANTED automatically for localhost: ip={}", clientIp);
            }
            return chain.filter(exchange);
        }

        // Check for private network ranges if allowed
        if (allowPrivateNetworks && isPrivateNetwork(clientIp)) {
            exchange.getAttributes().put(WHITELIST_ATTRIBUTE, true);
            if (isAdminPath(path) && !isExcluded(path)) {
                log.info("Admin access GRANTED for private network: ip={} path={}", clientIp, path);
            }
            return chain.filter(exchange);
        }

        return securityCacheService.isIpWhitelisted(clientIp)
                .flatMap(whitelisted -> {
                    if (whitelisted) {
                        // Tag for downstream filters (ThreatScoring, BotDetection, RateLimiter)
                        exchange.getAttributes().put(WHITELIST_ATTRIBUTE, true);
                    }

                    // 🛡️ Path-based Enforcement: Only protect /admin/** etc.
                    if (!isAdminPath(path) || isExcluded(path)) {
                        return chain.filter(exchange);
                    }

                    // Enforce for admin paths
                    loggingService.logAdminAccessAttempt(clientIp, path, method, whitelisted, userId);

                    if (whitelisted) {
                        log.info("Admin access GRANTED: ip={} path={}", clientIp, path);
                        return chain.filter(exchange);
                    } else {
                        log.warn("❌ Admin access DENIED: ip={} path={}. This IP is not in the whitelist.", clientIp, path);
                        // Return 404 to hide admin endpoint existence
                        return writeNotFound(exchange);
                    }
                })
                .onErrorResume(e -> {
                    // Fail-closed for admin paths, fail-open for public paths
                    if (isAdminPath(path) && !isExcluded(path)) {
                        log.error("Critical: Admin IP whitelist Redis error for ip={} on path {}: {}", clientIp, path, e.getMessage());
                        return writeNotFound(exchange);
                    }
                    return chain.filter(exchange);
                });
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isAdminPath(String path) {
        return PROTECTED_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }


    private boolean isPrivateNetwork(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        // Basic private network patterns
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.") || 
               ip.startsWith("172.19.") || ip.startsWith("172.2") || ip.startsWith("172.30.") || 
               ip.startsWith("172.31.") ||
               ip.startsWith("169.254.");
    }


    private Mono<Void> writeNotFound(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"success\":false,\"statusCode\":404,\"message\":\"Not Found\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
