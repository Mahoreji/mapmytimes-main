package in.mapmytour.customer.controller.career;

import in.mapmytour.customer.dto.APIResponse;
import in.mapmytour.customer.dto.career.JobApplicationResponse;
import in.mapmytour.customer.dto.career.JobApplicationSummaryResponse;
import in.mapmytour.customer.exception.AccessDeniedException;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/applications")
@Slf4j
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final UserContextService userContextService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<APIResponse<JobApplicationResponse>> applyForJob(
            @RequestParam("jobId") String jobId,
            @RequestParam("applicantName") String applicantName,
            @RequestParam("applicantEmail") String applicantEmail,
            @RequestParam("applicantPhone") String applicantPhone,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam(value = "currentCtc", required = false) String currentCtc,
            @RequestParam(value = "expectedCtc", required = false) String expectedCtc,
            @RequestParam(value = "noticePeriod", required = false) String noticePeriod,
            @RequestParam(value = "yearsOfExperience", required = false) Integer yearsOfExperience,
            @RequestParam("resume") MultipartFile resume) {
        
        String applicantId = userContextService.getCurrentUserId();
        if (applicantId == null) {
            throw new AccessDeniedException("Only logged in users can apply for jobs");
        }

        JobApplicationResponse response = jobApplicationService.submitApplication(
                jobId, applicantName, applicantEmail, applicantPhone, coverLetter,
                currentCtc, expectedCtc, noticePeriod, yearsOfExperience, resume, applicantId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.<JobApplicationResponse>builder()
                .success(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Application submitted successfully")
                .data(response)
                .build());
    }

    @GetMapping("/my")
    public ResponseEntity<APIResponse<Page<JobApplicationSummaryResponse>>> getMyApplications(
            @PageableDefault(size = 10) Pageable pageable) {
        
        String applicantId = userContextService.getCurrentUserId();
        if (applicantId == null) {
            throw new AccessDeniedException("User not authenticated");
        }

        Page<JobApplicationSummaryResponse> applications = jobApplicationService.getMyApplications(applicantId, pageable);
        
        return ResponseEntity.ok(APIResponse.<Page<JobApplicationSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("My applications fetched successfully")
                .data(applications)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<JobApplicationResponse>> getApplicationById(@PathVariable String id) {
        JobApplicationResponse application = jobApplicationService.getApplicationById(id, false);
        
        // Authorization check
        if (!userContextService.isOwner(application.getApplicantId())) {
            throw new AccessDeniedException("You are not authorized to view this application");
        }
        
        return ResponseEntity.ok(APIResponse.<JobApplicationResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Application details fetched successfully")
                .data(application)
                .build());
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<APIResponse<Void>> withdrawApplication(@PathVariable String id) {
        String applicantId = userContextService.getCurrentUserId();
        jobApplicationService.withdrawApplication(id, applicantId);
        
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Application withdrawn successfully")
                .build());
    }
}
