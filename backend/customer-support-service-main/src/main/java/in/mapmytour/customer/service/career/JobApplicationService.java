package in.mapmytour.customer.service.career;

import in.mapmytour.customer.dto.career.JobApplicationResponse;
import in.mapmytour.customer.dto.career.JobApplicationSummaryResponse;
import in.mapmytour.customer.dto.career.UpdateApplicationStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface JobApplicationService {

    JobApplicationResponse submitApplication(
            String jobId, String applicantName, String applicantEmail,
            String applicantPhone, String coverLetter, String currentCtc,
            String expectedCtc, String noticePeriod, Integer yearsOfExperience,
            MultipartFile resume, String applicantId
    );

    JobApplicationResponse getApplicationById(String id, boolean isAdmin);

    Page<JobApplicationSummaryResponse> getMyApplications(String applicantId, Pageable pageable);

    Page<JobApplicationResponse> getAllApplications(Pageable pageable, String status, String searchTerm);

    Page<JobApplicationResponse> getApplicationsByJob(String jobId, Pageable pageable, String status);

    JobApplicationResponse updateApplicationStatus(String id, UpdateApplicationStatusRequest request);

    void withdrawApplication(String id, String applicantId);

    void deleteApplication(String id);

    Map<String, Long> getApplicationStats();
}
