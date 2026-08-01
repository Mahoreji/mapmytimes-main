package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal representation of a circle member respecting privacy (no contact details).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripCircleMemberSummary {
    private String userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private boolean verified;
    private String role; // OWNER / MEMBER
}
