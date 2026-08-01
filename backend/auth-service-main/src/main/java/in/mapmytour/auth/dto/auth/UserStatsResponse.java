package in.mapmytour.auth.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsResponse {
    private String userId;
    private String email;
    private long totalLogins;
    private long activeTokens;
    private LocalDateTime accountCreated;
    private LocalDateTime lastLogin;
    private boolean isVerified;
    private boolean isActive;
    private int profileCompleteness;
}