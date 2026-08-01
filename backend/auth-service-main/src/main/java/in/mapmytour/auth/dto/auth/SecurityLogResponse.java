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
public class SecurityLogResponse {
    private String id;
    private String eventType;
    private String description;
    private String ipAddress;
    private String userAgent;
    private String location;
    private LocalDateTime createdAt;
    private String riskLevel;
}