package in.mapmytour.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DB-backed rate limit counter for trip circles features.
 */
@Entity
@Table(name = "rate_limit_counter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitCounter {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "bucket", length = 32, nullable = false)
    private String bucket;

    @Column(name = "window_start", nullable = false)
    private OffsetDateTime windowStart;

    @Column(name = "count", nullable = false)
    private int count;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
