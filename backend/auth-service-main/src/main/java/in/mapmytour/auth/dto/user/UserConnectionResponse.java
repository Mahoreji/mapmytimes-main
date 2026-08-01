package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConnectionResponse {
    private String connectionId;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private LocalDateTime connectedAt;
    private String connectionType;
    /**
     * Last time this user was seen active (approximate).
     */
    private LocalDateTime lastSeenAt;
    private boolean isOnline;
}