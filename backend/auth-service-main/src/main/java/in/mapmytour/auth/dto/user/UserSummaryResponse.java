package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String businessName;
    private String avatarUrl;
    private String role;
}
