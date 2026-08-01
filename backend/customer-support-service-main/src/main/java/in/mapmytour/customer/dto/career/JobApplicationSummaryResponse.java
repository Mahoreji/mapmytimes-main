package in.mapmytour.customer.dto.career;

import in.mapmytour.customer.entity.career.JobApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationSummaryResponse {

    private String id;
    private String jobId;
    private String jobTitle;
    private String applicantName;
    private String applicantEmail;
    private JobApplication.ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
