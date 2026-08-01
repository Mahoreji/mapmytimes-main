package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vote on a circle poll.
 */
@Entity
@Table(name = "circle_poll_vote")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CirclePollVote {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private CirclePoll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private CirclePollOption option;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "voted_at", nullable = false)
    private OffsetDateTime votedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (votedAt == null) {
            votedAt = OffsetDateTime.now();
        }
    }
}
