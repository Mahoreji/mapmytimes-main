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
 * Stores the last booking-related click for a user within a circle
 * to support simple last-click attribution.
 */
@Entity
@Table(name = "circle_last_click")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleLastClick {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "circle_id", length = 36, nullable = false)
    private String circleId;

    @Column(name = "post_id", length = 36)
    private String postId;

    @Column(name = "ref_user_id", length = 36)
    private String refUserId;

    @Column(name = "clicked_at", nullable = false)
    private OffsetDateTime clickedAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (clickedAt == null) {
            clickedAt = OffsetDateTime.now();
        }
    }
}
