package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackActivityRequest {
    @NotBlank(message = "Activity type is required")
    private String activityType; // e.g., PACKAGE_VIEWED, PACKAGE_CLICKED, PAGE_VIEWED, BUTTON_CLICKED

    private String description; // Human-readable description

    private String metadata; // JSON string containing additional data like packageId, packageName, pageUrl, etc.
    
    // Example metadata JSON:
    // {"packageId": "123", "packageName": "Golden Triangle Tour", "packagePrice": 50000, "pageUrl": "/packages/golden-triangle"}
}

