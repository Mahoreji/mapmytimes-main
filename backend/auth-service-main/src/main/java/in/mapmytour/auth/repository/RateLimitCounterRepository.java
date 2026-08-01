package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.RateLimitCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface RateLimitCounterRepository extends JpaRepository<RateLimitCounter, String> {

    Optional<RateLimitCounter> findByUserIdAndBucketAndWindowStart(String userId, String bucket, OffsetDateTime windowStart);
}
