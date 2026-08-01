package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacySettingsRequest {
    private Boolean profileVisible;
    private Boolean showBookingHistory;
    private Boolean allowMessages;
    private Boolean showOnlineStatus;
    private Boolean dataCollection;
}