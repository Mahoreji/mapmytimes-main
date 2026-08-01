package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSuggestionResponse {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String suggestionReason;
    private int mutualConnections;
    private int totalConnections; // Total number of connections this user has
    private double matchScore;
}