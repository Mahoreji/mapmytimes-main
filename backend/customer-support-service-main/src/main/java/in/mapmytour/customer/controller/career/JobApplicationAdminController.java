package in.mapmytour.customer.controller.career;

import in.mapmytour.customer.dto.APIResponse;
import in.mapmytour.customer.dto.career.JobApplicationResponse;
import in.mapmytour.customer.dto.career.UpdateApplicationStatusRequest;
import in.mapmytour.customer.entity.career.JobApplication;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.exception.ServiceException;
import in.mapmytour.customer.service.UserContextService;
import in.mapmytour.customer.service.career.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/applications")
@Slf4j
@RequiredArgsConstructor
public class JobApplicationAdminController {

    private final JobApplicationService jobApplicationService;
    private final UserContextService userContextService;

    private void validateAdmin() {
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException("Only administrators can perform this action");
        }
    }

    @GetMapping
    public ResponseEntity<APIResponse<Page<JobApplicationResponse>>> getAllApplications(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchTerm) {
        
        validateAdmin();
        log.info("Admin fetching all applications");
        Page<JobApplicationResponse> applications = jobApplicationService.getAllApplications(pageable, status, searchTerm);
        
        return ResponseEntity.ok(APIResponse.<Page<JobApplicationResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("All applications fetched successfully")
                .data(applications)
                .build());
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<APIResponse<Page<JobApplicationResponse>>> getApplicationsByJob(
            @PathVariable String jobId,
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String status) {
        
        validateAdmin();
        log.info("Admin fetching applications for job: {}", jobId);
        Page<JobApplicationResponse> applications = jobApplicationService.getApplicationsByJob(jobId, pageable, status);
        
        return ResponseEntity.ok(APIResponse.<Page<JobApplicationResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Applications fetched successfully")
                .data(applications)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<JobApplicationResponse>> getApplicationById(@PathVariable String id) {
        validateAdmin();
        log.info("Admin fetching application details: {}", id);
        JobApplicationResponse application = jobApplicationService.getApplicationById(id, true);
        
        return ResponseEntity.ok(APIResponse.<JobApplicationResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Application details fetched successfully")
                .data(application)
                .build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<APIResponse<JobApplicationResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) UpdateApplicationStatusRequest request) {
        
        validateAdmin();
        log.info("Admin updating application status: {}", id);

        UpdateApplicationStatusRequest finalRequest = request;
        
        // If body is missing but status param is present, construct request from param
        if (finalRequest == null && status != null) {
            try {
                finalRequest = UpdateApplicationStatusRequest.builder()
                        .status(JobApplication.ApplicationStatus.valueOf(status.toUpperCase()))
                        .build();
            } catch (IllegalArgumentException e) {
                throw new ServiceException("Invalid status: " + status);
            }
        }

        if (finalRequest == null) {
            throw new ServiceException("Either request body or status query parameter is required");
        }

        JobApplicationResponse response = jobApplicationService.updateApplicationStatus(id, finalRequest);
        
        return ResponseEntity.ok(APIResponse.<JobApplicationResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Application status updated successfully")
                .data(response)
                .build());
    }

    @GetMapping("/stats")
    public ResponseEntity<APIResponse<Map<String, Long>>> getStats() {
        validateAdmin();
        log.info("Admin fetching application statistics");
        Map<String, Long> stats = jobApplicationService.getApplicationStats();
        
        return ResponseEntity.ok(APIResponse.<Map<String, Long>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Application statistics fetched successfully")
                .data(stats)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteApplication(@PathVariable String id) {
        validateAdmin();
        log.info("Admin deleting application: {}", id);
        jobApplicationService.deleteApplication(id);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Application deleted successfully")
                .build());
    }
}
