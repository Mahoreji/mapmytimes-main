package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String businessName;
    private String phone;
    private String avatarUrl;
    private String bio; // User biography/description
    private String coverImageUrl; // Profile cover image
    private LocalDate dateOfBirth;
    private String gender;
    private AddressResponse address;
    private UserPreferencesResponse preferences;
    private String role;
    private boolean isVerified;
    private String createdAt;
    private String updatedAt;
    private boolean isOnline;
    private String lastSeenAt;

    /**
     * Total number of active connections for this user.
     * Only visible when viewing own profile or if profile is public.
     */
    private Integer totalConnections;

    /**
     * Number of mutual connections between the viewed user and the current user.
     * Only shown when currentUser is provided and is not the same as the viewed
     * user.
     */
    private Integer mutualConnections;
    /**
     * Role-specific details (e.g., supplier or agent metadata).
     * Fetched synchronously from specialized services.
     */
    private java.util.Map<String, Object> roleSpecificDetails;
}
