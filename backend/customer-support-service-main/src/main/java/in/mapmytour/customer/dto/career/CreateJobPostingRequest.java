package in.mapmytour.customer.dto.career;

import in.mapmytour.customer.entity.career.JobPosting;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobPostingRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Job type is required")
    private JobPosting.JobType jobType;

    @NotNull(message = "Experience level is required")
    private JobPosting.ExperienceLevel experienceLevel;

    @NotBlank(message = "Description is required")
    private String description;

    private String requirements;

    private String responsibilities;

    private Long salaryMin;

    private Long salaryMax;

    @Builder.Default
    private String salaryCurrency = "INR";

    private LocalDate applicationDeadline;

    @Builder.Default
    private boolean isActive = true;
}
