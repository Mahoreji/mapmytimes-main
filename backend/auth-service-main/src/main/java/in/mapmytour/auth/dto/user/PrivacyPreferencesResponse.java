package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyPreferencesResponse {
    private boolean profileVisible;
    private boolean showBookingHistory;
    
    // Granular field-level privacy settings
    private boolean showEmail;
    private boolean showPhone;
    private boolean showDateOfBirth;
    private boolean showAddress;
    private boolean showStreet;
    private boolean showCity;
    private boolean showState;
    private boolean showPostalCode;
}
