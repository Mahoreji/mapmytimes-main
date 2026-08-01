package in.mapmytour.blog.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String role;
    private boolean isVerified;
}
