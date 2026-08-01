package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPreferencesRequest {
    private String budgetRange;
    private List<String> preferredTransport;
    private String accommodationType;
    private List<String> dietaryRestrictions;
    private List<String> accessibilityNeeds;
    private String travelStyle;
    private List<String> preferredActivities;
}