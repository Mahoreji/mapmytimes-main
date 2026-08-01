package in.mapmytour.customer.entity.career;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a Job Application in the Career module.
 * Table: job_applications
 */
@Entity
@Table(name = "job_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String jobId;

    @Column(nullable = false)
    private String applicantId;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false)
    private String applicantEmail;

    @Column(nullable = false)
    private String applicantPhone;

    private String resumeUrl;

    private String resumeS3Key;

    private String resumeOriginalFileName;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    private String currentCtc;

    private String expectedCtc;

    private String noticePeriod;

    private Integer yearsOfExperience;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    private String rejectionReason;

    private LocalDateTime interviewScheduledAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ApplicationStatus {
        APPLIED, UNDER_REVIEW, SHORTLISTED, INTERVIEW,
        INTERVIEW_SCHEDULED, REJECTED, SELECTED, WITHDRAWN
    }
}
