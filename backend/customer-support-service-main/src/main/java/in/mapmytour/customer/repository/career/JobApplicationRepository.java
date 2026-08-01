package in.mapmytour.customer.repository.career;

import in.mapmytour.customer.entity.career.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, String> {

    Page<JobApplication> findByApplicantId(String applicantId, Pageable pageable);

    Page<JobApplication> findByJobId(String jobId, Pageable pageable);

    Page<JobApplication> findByStatus(JobApplication.ApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByJobIdAndStatus(String jobId, JobApplication.ApplicationStatus status, Pageable pageable);

    List<JobApplication> findByApplicantIdAndStatus(String applicantId, JobApplication.ApplicationStatus status);

    boolean existsByJobIdAndApplicantId(String jobId, String applicantId);

    long countByJobId(String jobId);

    long countByStatus(JobApplication.ApplicationStatus status);

    long countByApplicantId(String applicantId);

    @Query("SELECT a.status, COUNT(a) FROM JobApplication a GROUP BY a.status")
    List<Object[]> countGroupByStatus();

    @Query(value = "SELECT * FROM job_applications a WHERE " +
            "(:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(CAST(a.applicant_name AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(CAST(a.applicant_email AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR a.status = :status)",
            countQuery = "SELECT count(*) FROM job_applications a WHERE " +
                    "(:searchTerm IS NULL OR :searchTerm = '' OR " +
                    "LOWER(CAST(a.applicant_name AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(CAST(a.applicant_email AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
                    "AND (:status IS NULL OR :status = '' OR a.status = :status)",
            nativeQuery = true)
    Page<JobApplication> searchApplications(@Param("searchTerm") String searchTerm, @Param("status") String status, Pageable pageable);
}
