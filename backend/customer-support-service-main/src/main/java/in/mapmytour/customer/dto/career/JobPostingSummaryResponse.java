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
public class JobPostingSummaryResponse {

    private String id;
    private String title;
    private String department;
    private String location;
    private JobPosting.JobType jobType;
    private JobPosting.ExperienceLevel experienceLevel;
    private Long salaryMin;
    private Long salaryMax;
    private String salaryCurrency;
    private boolean isActive;
    private LocalDate applicationDeadline;
    private LocalDateTime createdAt;
}
