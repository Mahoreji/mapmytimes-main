package in.mapmytour.api.config;

import in.mapmytour.api.service.SecurityCacheService;
import in.mapmytour.api.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Custom Actuator Health Indicator for the Security Platform.
 * Exposes health state for the real-time security pipeline (Redis) 
 * and persistent security store (PostgreSQL).
 */
@Component
@RequiredArgsConstructor
public class SecurityHealthIndicator implements ReactiveHealthIndicator {

    private final SecurityCacheService securityCacheService;
    private final SecurityEventRepository securityEventRepository;

    @Override
    public Mono<Health> health() {
        return securityCacheService.isRedisHealthy()
                .flatMap(redisHealthy -> {
                    if (!redisHealthy) {
                        return Mono.just(Health.down()
                                .withDetail("security_cache", "DISCONNECTED")
                                .withDetail("reason", "Redis connection failure")
                                .build());
                    }

                    return securityEventRepository.count()
                            .map(count -> Health.up()
                                    .withDetail("security_cache", "CONNECTED")
                                    .withDetail("security_database", "CONNECTED")
                                    .withDetail("audit_log_count", count)
                                    .build())
                            .onErrorResume(e -> Mono.just(Health.status("DEGRADED")
                                    .withDetail("security_cache", "CONNECTED")
                                    .withDetail("security_database", "DISCONNECTED")
                                    .withDetail("reason", e.getMessage())
                                    .build()));
                });
    }
}
