package in.mapmytour.api.controller.admin;

import in.mapmytour.api.dto.APIResponse;
import in.mapmytour.api.service.SecurityCacheService;
import in.mapmytour.api.service.SecurityLoggingService;
import in.mapmytour.api.utils.IpUtils;
import in.mapmytour.api.utils.GatewayJwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * SecurityAdminController — REST API for dynamic security management.
 *
 * All endpoints under /api/v1/admin/security/** are additionally protected by
 * AdminIpWhitelistFilter at the gateway level, so callers must be IP-whitelisted
 * before JWT/role checks even apply.
 *
 * Endpoints:
 *   POST   /api/v1/admin/security/ip-whitelist         Add an IP to whitelist
 *   GET    /api/v1/admin/security/ip-whitelist/{ip}    Check whitelist status
 *   DELETE /api/v1/admin/security/ip-whitelist/{ip}    Remove from whitelist
 *   POST   /api/v1/admin/security/whitelist-my-ip      Whitelist caller's own IP
 *   GET    /api/v1/admin/security/threat-score/{ip}    View threat score
 *   DELETE /api/v1/admin/security/bot-ban/{ip}         Lift bot ban
 */
@RestController
@RequestMapping("/api/v1/admin/security")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Security Administration", description = "Endpoints for managing the API gateway security platform (IP whitelisting, bot bans, threat scoring)")
public class SecurityAdminController {

    private final SecurityCacheService securityCacheService;
    private final SecurityLoggingService loggingService;
    private final GatewayJwtUtil jwtUtil;

    @Value("${security.admin.ip-whitelist.default-ttl-days:30}")
    private long defaultWhitelistTtlDays;

    // ─────────────────────────────────────────────────────────────────────────
    // IP Whitelist Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Add an IP address to the admin whitelist.
     *
     * Request body:
     * {
     *   "ip": "203.0.113.42",       // required
     *   "ttlDays": 7,               // optional, default 30
     *   "reason": "DevOps laptop"   // optional
     * }
     */
    @Operation(summary = "Add an IP to the admin whitelist", description = "Permanently whitelists an IP address for access to administrative routes.")
    @PostMapping({"/ip-whitelist", "/whitelist"})
    public Mono<ResponseEntity<APIResponse<Object>>> addToWhitelist(
            @RequestBody WhitelistRequest request,
            ServerWebExchange exchange) {

        return validateSuperAdmin(exchange)
                .flatMap(adminUser -> {
                    if (request.getIp() == null || request.getIp().isBlank()) {
                        return Mono.just(ResponseEntity.badRequest()
                                .body(APIResponse.builder()
                                        .success(false)
                                        .statusCode(400)
                                        .message("IP address is required")
                                        .build()));
                    }

                    Long ttlDays = request.getTtlDays();
                    Duration duration = (ttlDays != null && ttlDays > 0) ? Duration.ofDays(ttlDays) : 
                                       (ttlDays != null && ttlDays < 0) ? null : 
                                       Duration.ofDays(defaultWhitelistTtlDays);

                    String reason = request.getRemarks() != null ? request.getRemarks() : 
                                   (request.getReason() != null ? request.getReason() : "Manual Whitelist");

                    String msg = duration == null ? "IP permanently whitelisted" : "IP whitelisted for " + (ttlDays != null ? ttlDays : defaultWhitelistTtlDays) + " days";

                    return securityCacheService.whitelistIp(request.getIp(), duration, adminUser.getUserId(), reason)
                            .doOnSuccess(r -> loggingService.logIpWhitelistChange(IpUtils.resolveClientIp(exchange.getRequest()), request.getIp(), "ADD"))
                            .map(success -> ResponseEntity.ok(APIResponse.builder()
                                    .success(true)
                                    .statusCode(200)
                                    .message(msg)
                                    .data(Map.of(
                                            "ip", request.getIp(),
                                            "expiryAt", duration == null ? "PERMANENT" : "SET",
                                            "reason", reason,
                                            "addedAt", Instant.now().toString()
                                    ))
                                    .build()));
                });
    }

    /**
     * List all currently whitelisted IP addresses.
     */
    @Operation(summary = "List all whitelisted IPs", description = "Retrieves the full list of active whitelisted IP addresses from the source of truth.")
    @GetMapping({"/ip-whitelist", "/whitelist/all"})
    public Mono<ResponseEntity<APIResponse<Object>>> listAllWhitelisted(ServerWebExchange exchange) {
        return validateAdmin(exchange)
                .flatMap(admin -> securityCacheService.getAllWhitelistedIps()
                        .collectList()
                        .map(list -> ResponseEntity.ok(APIResponse.builder()
                                .success(true)
                                .statusCode(200)
                                .message("Whitelisted IPs retrieved successfully")
                                .data(Map.of(
                                        "count", list.size(),
                                        "ips", list
                                ))
                                .build())));
    }

    /**
     * Check whether a given IP is currently whitelisted.
     */
    @Operation(summary = "Check IP whitelist status", description = "Verifies if the specified IP address is currently whitelisted for admin access.")
    @GetMapping({"/ip-whitelist/{ip}", "/whitelist/{ip}"})
    public Mono<ResponseEntity<APIResponse<Object>>> checkWhitelist(@PathVariable String ip) {
        return securityCacheService.isIpWhitelisted(ip)
                .map(whitelisted -> ResponseEntity.ok(APIResponse.builder()
                        .success(true)
                        .statusCode(200)
                        .message("Whitelist status retrieved")
                        .data(Map.of(
                                "ip", ip,
                                "whitelisted", whitelisted
                        ))
                        .build()));
    }

    /**
     * Remove an IP from the admin whitelist immediately.
     */
    @Operation(summary = "Remove an IP from the whitelist", description = "Immediately revokes admin access for the specified IP address.")
    @DeleteMapping({"/ip-whitelist/{ip}", "/whitelist/{ip}"})
    public Mono<ResponseEntity<APIResponse<Object>>> removeFromWhitelist(
            @PathVariable String ip,
            ServerWebExchange exchange) {

        return validateSuperAdmin(exchange)
                .flatMap(admin -> {
                    String adminIp = IpUtils.resolveClientIp(exchange.getRequest());
                    return securityCacheService.removeIpFromWhitelist(ip)
                            .doOnSuccess(r -> loggingService.logIpWhitelistChange(adminIp, ip, "REMOVE"))
                            .then(Mono.just(ResponseEntity.ok(APIResponse.builder()
                                    .success(true)
                                    .statusCode(200)
                                    .message("IP removed from whitelist")
                                    .data(Map.of(
                                            "ip", ip,
                                            "removed", true,
                                            "removedAt", Instant.now().toString()
                                    ))
                                    .build())));
                });
    }

    /**
     * Convenience endpoint: whitelist the caller's own source IP.
     * Useful for bootstrapping admin access from a new environment.
     */
    @PostMapping({"/whitelist-my-ip", "/whitelist/me"})
    public Mono<ResponseEntity<APIResponse<Object>>> whitelistMyIp(
            @RequestParam(value = "ttlDays", required = false) Long ttlDays,
            ServerWebExchange exchange) {
        
        return validateAdminOrEmployee(exchange)
                .flatMap(admin -> {
                    String callerIp = IpUtils.resolveClientIp(exchange.getRequest());
                    Duration duration = (ttlDays != null && ttlDays > 0) ? Duration.ofDays(ttlDays) : 
                                       (ttlDays != null && ttlDays < 0) ? null : 
                                       Duration.ofDays(defaultWhitelistTtlDays);

                    String msg = duration == null ? "Your IP has been permanently whitelisted" : "Your IP has been whitelisted for " + (ttlDays != null ? ttlDays : defaultWhitelistTtlDays) + " days";

                    return securityCacheService.whitelistIp(callerIp, duration, admin.getUserId(), "Self-Bootstrap Whitelist")
                            .doOnSuccess(r -> loggingService.logIpWhitelistChange(callerIp, callerIp, "SELF_ADD"))
                            .map(success -> ResponseEntity.ok(APIResponse.builder()
                                    .success(true)
                                    .statusCode(200)
                                    .message(msg)
                                    .data(Map.of(
                                            "ip", callerIp,
                                            "expiryAt", duration == null ? "PERMANENT" : "SET",
                                            "addedAt", Instant.now().toString()
                                    ))
                                    .build()));
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Threat Score Inspection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get the current rolling threat score for a given IP.
     */
    @Operation(summary = "Get current threat score", description = "Retrieves the rolling behavioral threat score for any IP.")
    @GetMapping({"/threat-score/{ip}", "/risk/{ip}"})
    public Mono<ResponseEntity<APIResponse<Object>>> getThreatScore(
            @PathVariable String ip,
            ServerWebExchange exchange) {
        return validateAdmin(exchange)
                .flatMap(admin -> securityCacheService.getThreatScore(ip)
                        .map(score -> ResponseEntity.ok(APIResponse.builder()
                                .success(true)
                                .statusCode(200)
                                .message("Threat score retrieved")
                                .data(Map.of(
                                        "ip", ip,
                                        "threatScore", score,
                                        "level", scoreLevel(score)
                                ))
                                .build())));
    }

    /**
     * Get all active security threat scores for audit.
     */
    @Operation(summary = "Audit all active threat scores", description = "Retrieves the behavioral risk profile for all currently tracked IP addresses in the Redis cache.")
    @GetMapping({"/threat-scores", "/risks", "/risks/all"})
    public Mono<ResponseEntity<APIResponse<Object>>> getAllThreatScores(ServerWebExchange exchange) {
        return validateAdmin(exchange)
                .flatMap(admin -> securityCacheService.getAllThreatScores()
                        .map(scores -> ResponseEntity.ok(APIResponse.builder()
                                .success(true)
                                .statusCode(200)
                                .message("Active threat profiles audited successfully")
                                .data(Map.of(
                                        "count", scores.size(),
                                        "scores", scores
                                ))
                                .build())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bot Ban Management
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Manually lift an IP ban", description = "Clears any detected or manual ban for the specified IP address.")
    @DeleteMapping({"/bot-ban/{ip}", "/unban/{ip}", "/bans/{ip}", "/ban/{ip}"})
    public Mono<ResponseEntity<APIResponse<Object>>> liftBotBan(
            @PathVariable String ip,
            ServerWebExchange exchange) {
        return validateAdmin(exchange)
                .flatMap(admin -> securityCacheService.banIp(ip, Duration.ofSeconds(1))
                        .map(r -> ResponseEntity.ok(APIResponse.builder()
                                .success(true)
                                .statusCode(200)
                                .message("Bot ban lifted")
                                .data(Map.of(
                                        "ip", ip,
                                        "banLifted", true,
                                        "liftedAt", Instant.now().toString()
                                ))
                                .build())));
    }

    /**
     * Manually ban an IP address.
     */
    @Operation(summary = "Manually ban an IP", description = "Suspends all access for a specific IP address for a defined duration.")
    @PostMapping({"/bans", "/ban"})
    public Mono<ResponseEntity<APIResponse<Object>>> manualBan(
            @RequestBody BanRequest request,
            ServerWebExchange exchange) {
        return validateAdmin(exchange)
                .flatMap(admin -> {
                    if (request.getIp() == null || request.getIp().isBlank()) {
                        return Mono.just(ResponseEntity.badRequest().body(APIResponse.builder().success(false).message("IP required").build()));
                    }

                    long minutes = request.getExpiryMinutes() != null ? request.getExpiryMinutes() : 1440; // Default 1 day
                    return securityCacheService.banIp(request.getIp(), Duration.ofMinutes(minutes))
                            .map(success -> ResponseEntity.ok(APIResponse.builder()
                                    .success(true)
                                    .statusCode(200)
                                    .message("IP banned successfully")
                                    .data(Map.of(
                                            "ip", request.getIp(),
                                            "expiryMinutes", minutes,
                                            "reason", request.getReason() != null ? request.getReason() : "Manual ban"
                                    ))
                                    .build()));
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts and validates the JWT from the Authorization header.
     * Enforces strictly SUPER_ADMIN role.
     */
    private Mono<GatewayJwtUtil.UserContext> validateSuperAdmin(ServerWebExchange exchange) {
        return extractToken(exchange)
                .flatMap(token -> {
                    try {
                        io.jsonwebtoken.Claims claims = jwtUtil.getAllClaimsFromToken(token);
                        if (!jwtUtil.isValidForApiAccess(claims)) {
                            return Mono.error(new SecurityException("Invalid or expired session"));
                        }
                        GatewayJwtUtil.UserContext context = jwtUtil.extractUserContext(claims);
                        if (context == null || !"SUPER_ADMIN".equalsIgnoreCase(context.getRole())) {
                            return Mono.error(new SecurityException("Insufficient privileges: SUPER_ADMIN required"));
                        }
                        return Mono.just(context);
                    } catch (Exception e) {
                        return Mono.error(new SecurityException("JWT validation failed: " + e.getMessage()));
                    }
                })
                .onErrorResume(e -> Mono.error(new AuthenticationException(e.getMessage())));
    }

    /**
     * extracts and validates the JWT from the Authorization header.
     * Enforces ADMIN or SUPER_ADMIN role.
     */
    private Mono<GatewayJwtUtil.UserContext> validateAdmin(ServerWebExchange exchange) {
        return extractToken(exchange)
                .flatMap(token -> {
                    try {
                        io.jsonwebtoken.Claims claims = jwtUtil.getAllClaimsFromToken(token);
                        if (!jwtUtil.isValidForApiAccess(claims)) {
                            return Mono.error(new SecurityException("Invalid or expired session"));
                        }
                        GatewayJwtUtil.UserContext context = jwtUtil.extractUserContext(claims);
                        if (context == null || (!"ADMIN".equalsIgnoreCase(context.getRole()) && !"SUPER_ADMIN".equalsIgnoreCase(context.getRole()))) {
                            return Mono.error(new SecurityException("Insufficient privileges: ADMIN role required"));
                        }
                        return Mono.just(context);
                    } catch (Exception e) {
                        return Mono.error(new SecurityException("JWT validation failed: " + e.getMessage()));
                    }
                })
                .onErrorResume(e -> Mono.error(new AuthenticationException(e.getMessage())));
    }

    /**
     * Extracts and validates the JWT from the Authorization header.
     * Enforces ADMIN, SUPER_ADMIN or EMPLOYEE role.
     */
    private Mono<GatewayJwtUtil.UserContext> validateAdminOrEmployee(ServerWebExchange exchange) {
        return extractToken(exchange)
                .flatMap(token -> {
                    try {
                        io.jsonwebtoken.Claims claims = jwtUtil.getAllClaimsFromToken(token);
                        if (!jwtUtil.isValidForApiAccess(claims)) {
                            return Mono.error(new SecurityException("Invalid or expired session"));
                        }
                        GatewayJwtUtil.UserContext context = jwtUtil.extractUserContext(claims);
                        if (context == null || (!"ADMIN".equalsIgnoreCase(context.getRole()) && 
                                               !"SUPER_ADMIN".equalsIgnoreCase(context.getRole()) && 
                                               !"EMPLOYEE".equalsIgnoreCase(context.getRole()))) {
                            return Mono.error(new SecurityException("Insufficient privileges: ADMIN or EMPLOYEE role required"));
                        }
                        return Mono.just(context);
                    } catch (Exception e) {
                        return Mono.error(new SecurityException("JWT validation failed: " + e.getMessage()));
                    }
                })
                .onErrorResume(e -> Mono.error(new AuthenticationException(e.getMessage())));
    }

    private Mono<String> extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token == null) {
            return Mono.error(new AuthenticationException("Authorization token missing"));
        }
        return Mono.just(token);
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<APIResponse<Object>> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(401).body(APIResponse.builder()
                .success(false)
                .statusCode(401)
                .message(e.getMessage())
                .build());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<APIResponse<Object>> handleSecurity(SecurityException e) {
        return ResponseEntity.status(403).body(APIResponse.builder()
                .success(false)
                .statusCode(403)
                .message(e.getMessage())
                .build());
    }

    private static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) { super(message); }
    }

    private static class SecurityException extends RuntimeException {
        public SecurityException(String message) { super(message); }
    }

    private String scoreLevel(int score) {
        if (score >= 70) return "HIGH_RISK";
        if (score >= 30) return "MEDIUM_RISK";
        return "LOW_RISK";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTOs
    // ─────────────────────────────────────────────────────────────────────────

    @Data
    public static class WhitelistRequest {
        private String ip;
        private Long ttlDays;
        private String reason;
        private String remarks; // Compatibility alias
    }

    @Data
    public static class BanRequest {
        private String ip;
        private Long expiryMinutes;
        private String reason;
    }
}
