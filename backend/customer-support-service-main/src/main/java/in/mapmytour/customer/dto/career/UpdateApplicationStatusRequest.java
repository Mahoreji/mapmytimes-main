package in.mapmytour.customer.dto.career;

import in.mapmytour.customer.entity.career.JobApplication;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateApplicationStatusRequest {

    @NotNull(message = "Status is required")
    private JobApplication.ApplicationStatus status;

    private String adminNotes;

    private String rejectionReason;

    private LocalDateTime interviewScheduledAt;
}
