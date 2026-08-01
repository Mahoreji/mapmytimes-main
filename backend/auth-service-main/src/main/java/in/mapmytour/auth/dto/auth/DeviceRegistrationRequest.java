package in.mapmytour.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {

    @NotBlank(message = "Device name is required")
    private String deviceName;

    private String deviceType;
    private String osVersion;
    private String appVersion;
    private String fcmToken; // For push notifications
}
