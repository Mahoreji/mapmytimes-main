package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String ipAddress;

    @Column(length = 200)
    private String userAgent;

    @Column(length = 100)
    private String location;

    @Column(length = 50)
    private String deviceType; // e.g., MOBILE, DESKTOP, TABLET

    @Column(nullable = false)
    @Builder.Default
    private boolean successful = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime loginTime;
}

