package in.mapmytour.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Deduplication record for per-day micro-actions like TODAY_PLAN and CHECKIN_SAFE.
 */
@Entity
@Table(name = "user_action_dedup")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActionDedup {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "circle_id", length = 36, nullable = false)
    private String circleId;

    @Column(name = "action_type", length = 24, nullable = false)
    private String actionType;

    @Column(name = "action_date", nullable = false)
    private LocalDate actionDate;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
