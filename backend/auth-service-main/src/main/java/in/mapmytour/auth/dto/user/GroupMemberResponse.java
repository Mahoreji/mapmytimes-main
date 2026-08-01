package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberResponse {
    private String id;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String role; // CREATOR, ADMIN, MEMBER
    private String status; // ACTIVE, PENDING, LEFT, REMOVED
    private LocalDateTime joinedAt;
}

