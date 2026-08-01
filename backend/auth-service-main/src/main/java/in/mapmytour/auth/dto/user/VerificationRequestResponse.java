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
public class VerificationRequestResponse {
    
    private String id;
    private String userId;
    private String userEmail;
    private String userName;
    private String userAvatarUrl;
    private String verificationType;
    private String documentType;
    private String description;
    private String reason;
    private String status; // PENDING, APPROVED, REJECTED, WITHDRAWN
    private String adminNotes;
    private String reviewedBy;
    private String reviewedByEmail;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

