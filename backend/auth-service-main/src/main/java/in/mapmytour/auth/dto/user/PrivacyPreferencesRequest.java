package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyPreferencesRequest {

    private Boolean profileVisible;
    private Boolean showBookingHistory;
    
    // Granular field-level privacy settings (only applicable when profileVisible = true)
    private Boolean showEmail;
    private Boolean showPhone;
    private Boolean showDateOfBirth;
    private Boolean showAddress; // Controls all address fields
    private Boolean showStreet;
    private Boolean showCity;
    private Boolean showState;
    private Boolean showPostalCode;
}
