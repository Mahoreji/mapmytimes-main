package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_connections", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "connected_user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connected_user_id", nullable = false)
    private User connectedUser;

    @Column(length = 50)
    private String connectionType; // e.g., FRIEND, FOLLOWER, FOLLOWING

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;
}

