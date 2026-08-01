package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "recipient_user_id", nullable = false, length = 36)
    private String recipientUserId;

    @Column(name = "sender_user_id", nullable = false, length = 36)
    private String senderUserId;

    @Column(nullable = false, length = 50)
    private String type; // SOCIAL_LIKE, SOCIAL_COMMENT

    @Column(nullable = false)
    private String message;

    @Column(name = "post_id", length = 36)
    private String postId;

    @Column(name = "booking_id", length = 50)
    private String bookingId;

    @Column(name = "payment_id", length = 50)
    private String paymentId;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_avatar")
    private String userAvatar;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
