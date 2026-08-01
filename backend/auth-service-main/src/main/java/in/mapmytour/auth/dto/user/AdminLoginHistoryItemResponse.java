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
public class AdminLoginHistoryItemResponse {
    private String id;
    private String userEmail;
    private String userId;
    private String ipAddress;
    private String userAgent;
    private String location;
    private LocalDateTime loginTime;
    private String deviceType;
    private boolean successful;
}

