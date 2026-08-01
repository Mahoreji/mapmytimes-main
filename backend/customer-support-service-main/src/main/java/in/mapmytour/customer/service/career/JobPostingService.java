package in.mapmytour.customer.service.career;

import in.mapmytour.customer.dto.career.CreateJobPostingRequest;
import in.mapmytour.customer.dto.career.JobPostingResponse;
import in.mapmytour.customer.dto.career.JobPostingSearchRequest;
import in.mapmytour.customer.dto.career.JobPostingSummaryResponse;
import in.mapmytour.customer.dto.career.UpdateJobPostingRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface JobPostingService {

    JobPostingResponse createJobPosting(CreateJobPostingRequest request, String postedBy);

    JobPostingResponse getJobPostingById(String id);

    Page<JobPostingSummaryResponse> getAllJobPostings(JobPostingSearchRequest request);

    Page<JobPostingSummaryResponse> searchJobPostings(JobPostingSearchRequest request);

    JobPostingResponse updateJobPosting(String id, UpdateJobPostingRequest request);

    JobPostingResponse toggleJobStatus(String id);

    void deleteJobPosting(String id);

    List<String> getAllDepartments();

    long countActiveJobs();
}
