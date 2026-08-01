package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsRequest {
    private Boolean email;
    private Boolean sms;
    private Boolean push;
    private Boolean marketing;
    private Boolean security;
    private Boolean bookingUpdates;
    private Boolean newsletter;
}