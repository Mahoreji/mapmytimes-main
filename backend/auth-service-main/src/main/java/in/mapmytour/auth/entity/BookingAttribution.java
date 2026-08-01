package in.mapmytour.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Attribution record linking a booking to a trip circle and/or content.
 */
@Entity
@Table(name = "booking_attribution")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAttribution {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "booking_id", length = 36, nullable = false, unique = true)
    private String bookingId;

    @Column(name = "booker_user_id", length = 36, nullable = false)
    private String bookerUserId;

    @Column(name = "circle_id", length = 36)
    private String circleId;

    @Column(name = "post_id", length = 36)
    private String postId;

    @Column(name = "ref_user_id", length = 36)
    private String refUserId;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "eligible", nullable = false)
    @Builder.Default
    private boolean eligible = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
