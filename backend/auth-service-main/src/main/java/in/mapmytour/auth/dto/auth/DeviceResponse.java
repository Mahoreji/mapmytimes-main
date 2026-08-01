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
public class DeviceResponse {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime lastActive;
    private boolean isCurrentDevice;
}