package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesRequest {

    private NotificationPreferencesRequest notifications;
    private PrivacyPreferencesRequest privacy;

    @Size(max = 20, message = "Maximum 20 interests allowed")
    private List<@NotBlank @Size(max = 50) String> interests;
}
