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
public class JobApplicationResponse {

    private String id;
    private String jobId;
    private String jobTitle; // Fetched from JobPosting
    private String applicantId;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String resumeUrl;
    private String resumeS3Key;
    private String resumeOriginalFileName;
    private String coverLetter;
    private JobApplication.ApplicationStatus status;
    private String currentCtc;
    private String expectedCtc;
    private String noticePeriod;
    private Integer yearsOfExperience;
    private String adminNotes; // Hide for non-admins
    private String rejectionReason;
    private LocalDateTime interviewScheduledAt;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
