package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Real-time presence update (online/offline, last seen)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceUpdateResponse {
    private String userId;
    private String email;
    private boolean isOnline;
    private LocalDateTime lastSeenAt;
    private String status; // ONLINE, OFFLINE, AWAY, TYPING
}

