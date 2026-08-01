package in.mapmytour.api.controller.admin;

import in.mapmytour.api.dto.APIResponse;
import in.mapmytour.api.dto.response.SecurityTelemetryResponse;
import in.mapmytour.api.repository.IpWhitelistRepository;
import in.mapmytour.api.repository.SecurityEventRepository;
import in.mapmytour.api.service.SecurityCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * API for real-time security telemetry and production monitoring.
 */
@RestController
@RequestMapping("/api/v1/admin/security/telemetry")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Security Telemetry", description = "Real-time monitoring and production health of the security gateway")
public class SecurityMetricsController {

    private final SecurityEventRepository securityEventRepository;
    private final IpWhitelistRepository ipWhitelistRepository;
    private final SecurityCacheService securityCacheService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Operation(summary = "Get high-level security stats", description = "Provides real-time telemetry on blocked requests, whitelists, and system health.")
    @GetMapping("/stats")
    public Mono<ResponseEntity<APIResponse<SecurityTelemetryResponse>>> getSecurityStats() {
        log.info("Production Security: Generating real-time telemetry report...");

        // Complex aggregation would typically happen in a more robust way, 
        // but for this MVP telemetry we'll aggregate basic counts.
        
        return Mono.zip(
                securityEventRepository.count(),
                ipWhitelistRepository.count(),
                // In a real prod environment we'd use Redis SCAN or dedicated counters
                Mono.just(0L), // Active bot bans placeholder
                securityCacheService.isRedisHealthy()
        ).map(tuple -> {
            SecurityTelemetryResponse stats = SecurityTelemetryResponse.builder()
                    .totalBlockedRequests(tuple.getT1())
                    .whitelistedIpCount(tuple.getT2())
                    .activeBotBans(tuple.getT3())
                    .systemStatus(tuple.getT4() ? "HEALTHY" : "DEGRADED")
                    .databaseStatus("CONNECTED")
                    .redisStatus(tuple.getT4() ? "CONNECTED" : "DISCONNECTED")
                    .eventsByAction(new HashMap<>()) // Simplified for now
                    .topAttackers(new ArrayList<>())
                    .build();

            return ResponseEntity.ok(APIResponse.<SecurityTelemetryResponse>builder()
                    .success(true)
                    .statusCode(200)
                    .message("Security telemetry generated successfully")
                    .data(stats)
                    .build());
        });
    }
}
