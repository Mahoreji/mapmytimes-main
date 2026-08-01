package in.mapmytour.customer.dto.career;

import in.mapmytour.customer.entity.career.JobPosting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingResponse {

    private String id;
    private String title;
    private String department;
    private String location;
    private JobPosting.JobType jobType;
    private JobPosting.ExperienceLevel experienceLevel;
    private String description;
    private String requirements;
    private String responsibilities;
    private Long salaryMin;
    private Long salaryMax;
    private String salaryCurrency;
    private boolean isActive;
    private LocalDate applicationDeadline;
    private String postedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long totalApplications;
}
