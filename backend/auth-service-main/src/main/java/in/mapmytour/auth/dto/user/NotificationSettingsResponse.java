package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsResponse {
    private boolean email;
    private boolean sms;
    private boolean push;
    private boolean marketing;
    private boolean security;
    private boolean bookingUpdates;
    private boolean newsletter;
}