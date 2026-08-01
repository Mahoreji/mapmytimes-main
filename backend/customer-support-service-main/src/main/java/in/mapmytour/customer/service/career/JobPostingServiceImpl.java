package in.mapmytour.customer.service.career;

import in.mapmytour.customer.entity.career.JobPosting;
import in.mapmytour.customer.repository.career.JobPostingRepository;
import in.mapmytour.customer.dto.career.CreateJobPostingRequest;
import in.mapmytour.customer.dto.career.JobPostingResponse;
import in.mapmytour.customer.dto.career.JobPostingSummaryResponse;
import in.mapmytour.customer.dto.career.UpdateJobPostingRequest;
import in.mapmytour.customer.dto.career.JobPostingSearchRequest;
import in.mapmytour.customer.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepository jobPostingRepository;

    @Override
    @Transactional
    public JobPostingResponse createJobPosting(CreateJobPostingRequest request, String postedBy) {
        log.info("Creating new job posting: {} by {}", request.getTitle(), postedBy);
        
        JobPosting jobPosting = JobPosting.builder()
                .title(request.getTitle())
                .department(request.getDepartment())
                .location(request.getLocation())
                .jobType(request.getJobType())
                .experienceLevel(request.getExperienceLevel())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .responsibilities(request.getResponsibilities())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency())
                .applicationDeadline(request.getApplicationDeadline())
                .isActive(request.isActive())
                .postedBy(postedBy)
                .build();

        JobPosting savedJob = jobPostingRepository.save(jobPosting);
        return mapToResponse(savedJob);
    }

    @Override
    public JobPostingResponse getJobPostingById(String id) {
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with id: " + id));
        return mapToResponse(jobPosting);
    }

    @Override
    public Page<JobPostingSummaryResponse> getAllJobPostings(JobPostingSearchRequest request) {
        log.info("Fetching job postings with filters: {}", request);
        
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Boolean activeOnly = request.getActiveOnly();

        Page<JobPosting> jobs;
        if (activeOnly != null) {
            // Filter by isActive
            if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
                jobs = jobPostingRepository.findByDepartmentAndIsActive(request.getDepartment(), activeOnly, pageable);
            } else if (request.getJobType() != null && !request.getJobType().isBlank()) {
                jobs = jobPostingRepository.findByJobTypeAndIsActive(JobPosting.JobType.valueOf(request.getJobType()), activeOnly, pageable);
            } else if (request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank()) {
                jobs = jobPostingRepository.findByExperienceLevelAndIsActive(JobPosting.ExperienceLevel.valueOf(request.getExperienceLevel()), activeOnly, pageable);
            } else {
                jobs = jobPostingRepository.findByIsActive(activeOnly, pageable);
            }
        } else {
            // Show all (active and inactive)
            if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
                jobs = jobPostingRepository.findByDepartment(request.getDepartment(), pageable);
            } else if (request.getJobType() != null && !request.getJobType().isBlank()) {
                jobs = jobPostingRepository.findByJobType(JobPosting.JobType.valueOf(request.getJobType()), pageable);
            } else if (request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank()) {
                jobs = jobPostingRepository.findByExperienceLevel(JobPosting.ExperienceLevel.valueOf(request.getExperienceLevel()), pageable);
            } else {
                jobs = jobPostingRepository.findAll(pageable);
            }
        }
        
        return jobs.map(this::mapToSummary);
    }

    @Override
    public Page<JobPostingSummaryResponse> searchJobPostings(JobPostingSearchRequest request) {
        log.info("Searching job postings with keyword: {}", request.getSearchTerm());
        
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        boolean activeOnly = request.getActiveOnly() != null ? request.getActiveOnly() : true;

        return jobPostingRepository.searchJobPostings(request.getSearchTerm(), activeOnly, pageable)
                .map(this::mapToSummary);
    }

    @Override
    @Transactional
    public JobPostingResponse updateJobPosting(String id, UpdateJobPostingRequest request) {
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with id: " + id));

        if (request.getTitle() != null) jobPosting.setTitle(request.getTitle());
        if (request.getDepartment() != null) jobPosting.setDepartment(request.getDepartment());
        if (request.getLocation() != null) jobPosting.setLocation(request.getLocation());
        if (request.getJobType() != null) jobPosting.setJobType(request.getJobType());
        if (request.getExperienceLevel() != null) jobPosting.setExperienceLevel(request.getExperienceLevel());
        if (request.getDescription() != null) jobPosting.setDescription(request.getDescription());
        if (request.getRequirements() != null) jobPosting.setRequirements(request.getRequirements());
        if (request.getResponsibilities() != null) jobPosting.setResponsibilities(request.getResponsibilities());
        if (request.getSalaryMin() != null) jobPosting.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) jobPosting.setSalaryMax(request.getSalaryMax());
        if (request.getSalaryCurrency() != null) jobPosting.setSalaryCurrency(request.getSalaryCurrency());
        if (request.getApplicationDeadline() != null) jobPosting.setApplicationDeadline(request.getApplicationDeadline());
        if (request.getIsActive() != null) jobPosting.setIsActive(request.getIsActive());

        jobPosting.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(jobPostingRepository.save(jobPosting));
    }

    @Override
    @Transactional
    public JobPostingResponse toggleJobStatus(String id) {
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with id: " + id));
        jobPosting.setIsActive(!jobPosting.isActive());
        jobPosting.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(jobPostingRepository.save(jobPosting));
    }

    @Override
    @Transactional
    public void deleteJobPosting(String id) {
        if (!jobPostingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job posting not found with id: " + id);
        }
        jobPostingRepository.deleteById(id);
    }

    @Override
    public List<String> getAllDepartments() {
        return jobPostingRepository.findAllActiveDepartments();
    }

    @Override
    public long countActiveJobs() {
        return jobPostingRepository.countByIsActive(true);
    }

    private JobPostingResponse mapToResponse(JobPosting job) {
        return JobPostingResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .department(job.getDepartment())
                .location(job.getLocation())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .responsibilities(job.getResponsibilities())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .isActive(job.isActive())
                .applicationDeadline(job.getApplicationDeadline())
                .postedBy(job.getPostedBy())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private JobPostingSummaryResponse mapToSummary(JobPosting job) {
        return JobPostingSummaryResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .department(job.getDepartment())
                .location(job.getLocation())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .isActive(job.isActive())
                .applicationDeadline(job.getApplicationDeadline())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
