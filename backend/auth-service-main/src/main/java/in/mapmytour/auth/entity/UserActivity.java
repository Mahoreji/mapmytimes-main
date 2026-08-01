package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String activityType; // e.g., LOGIN, LOGOUT, DOCUMENT_UPLOADED, PROFILE_UPDATED, etc.

    @Column(length = 200)
    private String description;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 200)
    private String userAgent;

    @Column(length = 50)
    private String status; // SUCCESS, FAILED, etc.

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

