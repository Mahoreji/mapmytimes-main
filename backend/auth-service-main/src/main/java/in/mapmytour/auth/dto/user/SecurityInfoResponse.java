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
public class SecurityInfoResponse {
    private int loginAttempts;
    private boolean isLocked;
    private LocalDateTime lockedUntil;
    private boolean twoFactorEnabled;
    private int activeTokens;
    private LocalDateTime lastPasswordChange;
}
