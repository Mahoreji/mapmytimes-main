package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Member of a {@link TripCircle}.
 */
@Entity
@Table(name = "trip_circle_member")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripCircleMember {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false)
    private TripCircle circle;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 16, nullable = false)
    @Builder.Default
    private TripCircleMemberRole role = TripCircleMemberRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }
}
