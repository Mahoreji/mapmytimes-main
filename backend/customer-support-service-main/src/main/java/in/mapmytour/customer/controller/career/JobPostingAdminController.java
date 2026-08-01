package in.mapmytour.customer.controller.career;

import in.mapmytour.customer.dto.APIResponse;
import in.mapmytour.customer.dto.career.CreateJobPostingRequest;
import in.mapmytour.customer.dto.career.JobPostingResponse;
import in.mapmytour.customer.dto.career.JobPostingSearchRequest;
import in.mapmytour.customer.dto.career.JobPostingSummaryResponse;
import in.mapmytour.customer.dto.career.UpdateJobPostingRequest;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.exception.ServiceException;
import in.mapmytour.customer.service.UserContextService;
import in.mapmytour.customer.service.career.JobPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/jobs")
@Slf4j
@RequiredArgsConstructor
public class JobPostingAdminController {

    private final JobPostingService jobPostingService;
    private final UserContextService userContextService;

    private void validateAdmin() {
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException("Only administrators can perform this action");
        }
    }
    
    @GetMapping
    public ResponseEntity<APIResponse<Page<JobPostingSummaryResponse>>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Boolean isActive) {
        
        validateAdmin();
        log.info("Admin fetching all job postings");
        
        JobPostingSearchRequest searchRequest = JobPostingSearchRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDir)
                .department(department)
                .jobType(jobType)
                .activeOnly(isActive) // Admin can filter by active or null for all
                .build();

        Page<JobPostingSummaryResponse> jobs = jobPostingService.getAllJobPostings(searchRequest);
        
        return ResponseEntity.ok(APIResponse.<Page<JobPostingSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Job postings fetched successfully for admin")
                .data(jobs)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<JobPostingResponse>> getJobById(@PathVariable String id) {
        validateAdmin();
        log.info("Admin fetching job details for id: {}", id);
        JobPostingResponse job = jobPostingService.getJobPostingById(id);
        return ResponseEntity.ok(APIResponse.<JobPostingResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Job details fetched successfully")
                .data(job)
                .build());
    }

    @PostMapping
    public ResponseEntity<APIResponse<JobPostingResponse>> createJob(
            @Valid @RequestBody CreateJobPostingRequest request) {
        
        validateAdmin();
        log.info("Admin creating job posting: {}", request.getTitle());
        String adminId = userContextService.getCurrentUserId();
        JobPostingResponse response = jobPostingService.createJobPosting(request, adminId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.<JobPostingResponse>builder()
                .success(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Job posting created successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<JobPostingResponse>> updateJob(
            @PathVariable String id,
            @Valid @RequestBody UpdateJobPostingRequest request) {
        
        validateAdmin();
        log.info("Admin updating job posting: {}", id);
        JobPostingResponse response = jobPostingService.updateJobPosting(id, request);
        return ResponseEntity.ok(APIResponse.<JobPostingResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Job posting updated successfully")
                .data(response)
                .build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<APIResponse<JobPostingResponse>> toggleJobStatus(@PathVariable String id) {
        validateAdmin();
        log.info("Admin toggling job status: {}", id);
        JobPostingResponse response = jobPostingService.toggleJobStatus(id);
        return ResponseEntity.ok(APIResponse.<JobPostingResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Job status updated successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<APIResponse<JobPostingResponse>> setJobStatus(
            @PathVariable String id,
            @RequestParam(required = false) Boolean active,
            @RequestBody(required = false) java.util.Map<String, Boolean> body) {
        
        validateAdmin();
        log.info("Admin setting job status: {}", id);

        Boolean isActive = active;
        if (isActive == null && body != null && body.containsKey("isActive")) {
            isActive = body.get("isActive");
        } else if (isActive == null && body != null && body.containsKey("active")) {
            isActive = body.get("active");
        }

        if (isActive == null) {
            throw new ServiceException("Either 'active' query parameter or request body with 'isActive' is required");
        }

        // We can use the existing toggle logic but only if it's currently different
        // Or better, let's just use the update method.
        // For simplicity, let's reuse updateJobPosting if possible, but that needs UpdateJobPostingRequest.
        // Let's just use the service directly if we can, or add a method.
        // JobPostingService has updateJobPosting.
        
        in.mapmytour.customer.dto.career.UpdateJobPostingRequest updateRequest = 
            new in.mapmytour.customer.dto.career.UpdateJobPostingRequest();
        updateRequest.setIsActive(isActive);
        
        JobPostingResponse response = jobPostingService.updateJobPosting(id, updateRequest);
        
        return ResponseEntity.ok(APIResponse.<JobPostingResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Job status set to " + (isActive ? "active" : "inactive"))
                .data(response)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteJob(@PathVariable String id) {
        validateAdmin();
        log.info("Admin deleting job posting: {}", id);
        jobPostingService.deleteJobPosting(id);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Job posting deleted successfully")
                .build());
    }
}
