package in.mapmytour.auth.dto.user;

import in.mapmytour.auth.dto.auth.UserStatsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailedResponse {
    private UserProfileResponse user;
    private UserStatsResponse stats;
    private SecurityInfoResponse securityInfo;
}