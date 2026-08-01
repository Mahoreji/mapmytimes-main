package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacySettingsResponse {
    private boolean profileVisible;
    private boolean showBookingHistory;
    private boolean allowMessages;
    private boolean showOnlineStatus;
    private boolean dataCollection;
}