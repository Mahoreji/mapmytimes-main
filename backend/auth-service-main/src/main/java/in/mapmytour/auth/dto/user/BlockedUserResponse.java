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
public class BlockedUserResponse {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private LocalDateTime blockedAt;
    private String reason;
}