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
public class VerificationStatusResponse {
    private boolean isVerified;
    private String verificationLevel;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private String status;
    private String rejectionReason;
}