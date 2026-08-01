package in.mapmytour.customer.controller.career;

import in.mapmytour.customer.dto.APIResponse;
import in.mapmytour.customer.dto.career.JobPostingResponse;
import in.mapmytour.customer.dto.career.JobPostingSearchRequest;
import in.mapmytour.customer.dto.career.JobPostingSummaryResponse;
import in.mapmytour.customer.service.career.JobPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@Slf4j
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @GetMapping
    public ResponseEntity<APIResponse<Page<JobPostingSummaryResponse>>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String experienceLevel) {
        
        JobPostingSearchRequest searchRequest = JobPostingSearchRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDir)
                .department(department)
                .jobType(jobType)
                .experienceLevel(experienceLevel)
                .activeOnly(true)
                .build();

        Page<JobPostingSummaryResponse> jobs = jobPostingService.getAllJobPostings(searchRequest);
        
        return ResponseEntity.ok(APIResponse.<Page<JobPostingSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Job postings fetched successfully")
                .data(jobs)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<APIResponse<Page<JobPostingSummaryResponse>>> searchJobs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        JobPostingSearchRequest searchRequest = JobPostingSearchRequest.builder()
                .searchTerm(query)
                .page(page)
                .size(size)
                .activeOnly(true)
                .build();

        Page<JobPostingSummaryResponse> jobs = jobPostingService.searchJobPostings(searchRequest);
        
        return ResponseEntity.ok(APIResponse.<Page<JobPostingSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Search results fetched successfully")
                .data(jobs)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<JobPostingResponse>> getJobById(@PathVariable String id) {
        JobPostingResponse job = jobPostingService.getJobPostingById(id);
        return ResponseEntity.ok(APIResponse.<JobPostingResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Job details fetched successfully")
                .data(job)
                .build());
    }

    @GetMapping("/departments")
    public ResponseEntity<APIResponse<List<String>>> getDepartments() {
        List<String> departments = jobPostingService.getAllDepartments();
        return ResponseEntity.ok(APIResponse.<List<String>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Departments fetched successfully")
                .data(departments)
                .build());
    }
}
