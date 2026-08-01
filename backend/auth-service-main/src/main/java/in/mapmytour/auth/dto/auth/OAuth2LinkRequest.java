package in.mapmytour.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2LinkRequest {

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Provider ID is required")
    private String providerId; // Fixed: was authorizationCode, now providerId

    // Optional fields for updating profile during linking
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String profileImageUrl; // Accept both avatarUrl and profileImageUrl (frontend compatibility)
    
    /**
     * Get avatar URL, preferring avatarUrl over profileImageUrl
     */
    public String getEffectiveAvatarUrl() {
        return avatarUrl != null && !avatarUrl.trim().isEmpty() 
            ? avatarUrl 
            : (profileImageUrl != null && !profileImageUrl.trim().isEmpty() ? profileImageUrl : null);
    }
}