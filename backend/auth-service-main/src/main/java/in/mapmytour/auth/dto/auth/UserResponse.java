package in.mapmytour.auth.dto.auth;

import in.mapmytour.auth.dto.user.AddressResponse;
import in.mapmytour.auth.dto.user.UserPreferencesResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private String dateOfBirth;
    private String gender;
    private AddressResponse address;
    private UserPreferencesResponse preferences;
    private String role;
    // roles array removed - only single role field used
    private String provider;
    private boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Role-specific details (e.g., supplier or agent metadata).
     * Fetched synchronously from specialized services.
     */
    private java.util.Map<String, Object> roleSpecificDetails;
}