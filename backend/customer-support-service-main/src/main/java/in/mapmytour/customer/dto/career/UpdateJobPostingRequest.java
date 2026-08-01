package in.mapmytour.customer.dto.career;

import in.mapmytour.customer.entity.career.JobPosting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateJobPostingRequest {

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
    private LocalDate applicationDeadline;
    private Boolean isActive;
}
