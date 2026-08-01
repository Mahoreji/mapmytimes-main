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
public class ConnectionRequestResponse {
    private String requestId;
    private String requesterId;
    private String requesterEmail;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterAvatarUrl;
    private String recipientId;
    private String recipientEmail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
