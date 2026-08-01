package in.mapmytour.api.repository;

import in.mapmytour.api.entity.IpWhitelistEntry;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Reactive R2DBC repository for persistent admin IP whitelist.
 */
@Repository
public interface IpWhitelistRepository extends ReactiveCrudRepository<IpWhitelistEntry, String> {

    @Query("SELECT * FROM gateway_ip_whitelist WHERE ip_address = :ip AND is_active = true AND (expires_at IS NULL OR expires_at > :now)")
    Mono<IpWhitelistEntry> findActiveByIp(String ip, LocalDateTime now);

    @Query("SELECT * FROM gateway_ip_whitelist WHERE is_active = :active")
    Flux<IpWhitelistEntry> findAllByActive(boolean active);

    @Query("SELECT * FROM gateway_ip_whitelist WHERE ip_address = :ipAddress")
    Mono<IpWhitelistEntry> findByIpAddress(String ipAddress);

    Mono<Void> deleteByIpAddress(String ipAddress);
}
