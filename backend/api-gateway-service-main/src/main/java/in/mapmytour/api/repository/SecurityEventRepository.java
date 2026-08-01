package in.mapmytour.api.repository;

import in.mapmytour.api.entity.SecurityEventLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Reactive R2DBC repository for security event audit logs.
 */
@Repository
public interface SecurityEventRepository extends ReactiveCrudRepository<SecurityEventLog, Long> {
}
