package in.mapmytour.customer.entity.career;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a Job Posting in the Career module.
 * Table: job_postings
 */
@Entity
@Table(name = "job_postings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperienceLevel experienceLevel;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    private Long salaryMin;

    private Long salaryMax;

    @Builder.Default
    @Column(length = 10)
    private String salaryCurrency = "INR";

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDate applicationDeadline;

    @Column(nullable = false)
    private String postedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Custom setter for Boolean field
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public enum JobType {
        FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT, FREELANCE
    }

    public enum ExperienceLevel {
        FRESHER, JUNIOR, MID, SENIOR, LEAD
    }
}
