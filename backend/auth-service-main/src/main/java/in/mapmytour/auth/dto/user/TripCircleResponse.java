package in.mapmytour.auth.dto.user;

import in.mapmytour.auth.entity.CircleStatus;
import in.mapmytour.auth.entity.CircleVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Response DTO representing a trip circle and the current user's membership.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripCircleResponse {

    private String id;
    private String destinationId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String createdByUserId;
    private CircleVisibility visibility;
    private CircleStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Membership info for current user
    private boolean joined;
    private String memberRole; // OWNER or MEMBER or null
    private int memberCount;
}
