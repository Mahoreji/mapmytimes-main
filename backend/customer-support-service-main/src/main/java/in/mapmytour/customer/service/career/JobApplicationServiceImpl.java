package in.mapmytour.customer.service.career;

import in.mapmytour.customer.entity.career.JobApplication;
import in.mapmytour.customer.entity.career.JobPosting;
import in.mapmytour.customer.repository.career.JobApplicationRepository;
import in.mapmytour.customer.repository.career.JobPostingRepository;
import in.mapmytour.customer.dto.career.JobApplicationResponse;
import in.mapmytour.customer.dto.career.JobApplicationSummaryResponse;
import in.mapmytour.customer.dto.career.UpdateApplicationStatusRequest;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.exception.ResourceNotFoundException;
import in.mapmytour.customer.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final S3CareerFileService s3CareerFileService;

    @Override
    @Transactional
    public JobApplicationResponse submitApplication(
            String jobId, String applicantName, String applicantEmail,
            String applicantPhone, String coverLetter, String currentCtc,
            String expectedCtc, String noticePeriod, Integer yearsOfExperience,
            MultipartFile resume, String applicantId
    ) {
        log.info("Submitting application for job: {} by {}", jobId, applicantEmail);

        // Check if job exists and is active
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found"));

        if (!job.isActive()) {
            throw new ServiceException("This job posting is no longer active");
        }

        // Check for duplicate application
        if (jobApplicationRepository.existsByJobIdAndApplicantId(jobId, applicantId)) {
            throw new ServiceException("You have already applied for this position");
        }

        // Upload resume to S3
        S3CareerFileService.ResumeUploadResult uploadResult = s3CareerFileService.uploadResume(resume, applicantId);

        JobApplication application = JobApplication.builder()
                .jobId(jobId)
                .applicantId(applicantId)
                .applicantName(applicantName)
                .applicantEmail(applicantEmail)
                .applicantPhone(applicantPhone)
                .resumeUrl(uploadResult.url())
                .resumeS3Key(uploadResult.s3Key())
                .resumeOriginalFileName(uploadResult.originalFileName())
                .coverLetter(coverLetter)
                .currentCtc(currentCtc)
                .expectedCtc(expectedCtc)
                .noticePeriod(noticePeriod)
                .yearsOfExperience(yearsOfExperience)
                .status(JobApplication.ApplicationStatus.APPLIED)
                .build();

        JobApplication savedApplication = jobApplicationRepository.save(application);
        return mapToResponse(savedApplication, job.getTitle());
    }

    @Override
    public JobApplicationResponse getApplicationById(String id, boolean isAdmin) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        JobPosting job = jobPostingRepository.findById(application.getJobId())
                .orElse(null);
        String jobTitle = (job != null) ? job.getTitle() : "N/A";

        JobApplicationResponse response = mapToResponse(application, jobTitle);
        
        // Hide admin notes for non-admins
        if (!isAdmin) {
            response.setAdminNotes(null);
        }
        
        return response;
    }

    @Override
    public Page<JobApplicationSummaryResponse> getMyApplications(String applicantId, Pageable pageable) {
        return jobApplicationRepository.findByApplicantId(applicantId, pageable)
                .map(app -> {
                    JobPosting job = jobPostingRepository.findById(app.getJobId()).orElse(null);
                    return mapToSummary(app, (job != null) ? job.getTitle() : "N/A");
                });
    }

    @Override
    public Page<JobApplicationResponse> getAllApplications(Pageable pageable, String status, String searchTerm) {
        return jobApplicationRepository.searchApplications(searchTerm, status, pageable)
                .map(app -> {
                    JobPosting job = jobPostingRepository.findById(app.getJobId()).orElse(null);
                    return mapToResponse(app, (job != null) ? job.getTitle() : "N/A");
                });
    }

    @Override
    public Page<JobApplicationResponse> getApplicationsByJob(String jobId, Pageable pageable, String status) {
        Page<JobApplication> apps;
        if (status != null && !status.isBlank()) {
            apps = jobApplicationRepository.findByJobIdAndStatus(jobId, JobApplication.ApplicationStatus.valueOf(status), pageable);
        } else {
            apps = jobApplicationRepository.findByJobId(jobId, pageable);
        }

        JobPosting job = jobPostingRepository.findById(jobId).orElse(null);
        String jobTitle = (job != null) ? job.getTitle() : "N/A";

        return apps.map(app -> mapToResponse(app, jobTitle));
    }

    @Override
    @Transactional
    public JobApplicationResponse updateApplicationStatus(String id, UpdateApplicationStatusRequest request) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        application.setStatus(request.getStatus());
        if (request.getAdminNotes() != null) application.setAdminNotes(request.getAdminNotes());
        if (request.getRejectionReason() != null) application.setRejectionReason(request.getRejectionReason());
        if (request.getInterviewScheduledAt() != null) application.setInterviewScheduledAt(request.getInterviewScheduledAt());
        
        application.setUpdatedAt(LocalDateTime.now());
        
        JobApplication saved = jobApplicationRepository.save(application);
        JobPosting job = jobPostingRepository.findById(saved.getJobId()).orElse(null);
        return mapToResponse(saved, (job != null) ? job.getTitle() : "N/A");
    }

    @Override
    @Transactional
    public void withdrawApplication(String id, String applicantId) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getApplicantId().equals(applicantId)) {
            throw new AccessDeniedException("You are not authorized to withdraw this application");
        }

        if (application.getStatus() == JobApplication.ApplicationStatus.SELECTED || 
            application.getStatus() == JobApplication.ApplicationStatus.REJECTED) {
            throw new ServiceException("Cannot withdraw application that is already " + application.getStatus());
        }

        application.setStatus(JobApplication.ApplicationStatus.WITHDRAWN);
        application.setUpdatedAt(LocalDateTime.now());
        jobApplicationRepository.save(application);
    }

    @Override
    @Transactional
    public void deleteApplication(String id) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Delete resume from S3
        s3CareerFileService.deleteResume(application.getResumeS3Key());
        
        jobApplicationRepository.delete(application);
    }

    @Override
    public Map<String, Long> getApplicationStats() {
        return jobApplicationRepository.countGroupByStatus().stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));
    }

    private JobApplicationResponse mapToResponse(JobApplication app, String jobTitle) {
        return JobApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJobId())
                .jobTitle(jobTitle)
                .applicantId(app.getApplicantId())
                .applicantName(app.getApplicantName())
                .applicantEmail(app.getApplicantEmail())
                .applicantPhone(app.getApplicantPhone())
                .resumeUrl(app.getResumeUrl())
                .resumeS3Key(app.getResumeS3Key())
                .resumeOriginalFileName(app.getResumeOriginalFileName())
                .coverLetter(app.getCoverLetter())
                .status(app.getStatus())
                .currentCtc(app.getCurrentCtc())
                .expectedCtc(app.getExpectedCtc())
                .noticePeriod(app.getNoticePeriod())
                .yearsOfExperience(app.getYearsOfExperience())
                .adminNotes(app.getAdminNotes())
                .rejectionReason(app.getRejectionReason())
                .interviewScheduledAt(app.getInterviewScheduledAt())
                .appliedAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private JobApplicationSummaryResponse mapToSummary(JobApplication app, String jobTitle) {
        return JobApplicationSummaryResponse.builder()
                .id(app.getId())
                .jobId(app.getJobId())
                .jobTitle(jobTitle)
                .applicantName(app.getApplicantName())
                .applicantEmail(app.getApplicantEmail())
                .status(app.getStatus())
                .appliedAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
