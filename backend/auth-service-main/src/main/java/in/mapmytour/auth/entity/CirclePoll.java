package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Poll inside a trip circle.
 */
@Entity
@Table(name = "circle_poll")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CirclePoll {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false)
    private TripCircle circle;

    @Column(name = "created_by_user_id", length = 36, nullable = false)
    private String createdByUserId;

    @Column(name = "question", length = 180, nullable = false)
    private String question;

    @Column(name = "closes_at")
    private OffsetDateTime closesAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    @Builder.Default
    private PollStatus status = PollStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
