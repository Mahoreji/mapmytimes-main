package in.mapmytour.customer.repository.career;

import in.mapmytour.customer.entity.career.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, String> {

    Page<JobPosting> findByIsActive(Boolean isActive, Pageable pageable);

    Page<JobPosting> findByDepartmentAndIsActive(String department, Boolean isActive, Pageable pageable);
    
    Page<JobPosting> findByDepartment(String department, Pageable pageable);

    Page<JobPosting> findByJobType(JobPosting.JobType jobType, Pageable pageable);

    Page<JobPosting> findByJobTypeAndIsActive(JobPosting.JobType jobType, Boolean isActive, Pageable pageable);

    Page<JobPosting> findByExperienceLevelAndIsActive(JobPosting.ExperienceLevel experienceLevel, Boolean isActive, Pageable pageable);

    Page<JobPosting> findByExperienceLevel(JobPosting.ExperienceLevel experienceLevel, Pageable pageable);

    Page<JobPosting> findByPostedBy(String postedBy, Pageable pageable);

    long countByIsActive(Boolean isActive);

    @Query("SELECT DISTINCT j.department FROM JobPosting j WHERE j.isActive = true ORDER BY j.department")
    List<String> findAllActiveDepartments();

    @Query(value = "SELECT * FROM job_postings j WHERE " +
            "(:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(CAST(j.title AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(CAST(j.department AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(CAST(j.location AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(CAST(j.description AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:activeOnly = false OR j.is_active = true)",
            countQuery = "SELECT count(*) FROM job_postings j WHERE " +
                    "(:searchTerm IS NULL OR :searchTerm = '' OR " +
                    "LOWER(CAST(j.title AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(CAST(j.department AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(CAST(j.location AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(CAST(j.description AS TEXT)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
                    "AND (:activeOnly = false OR j.is_active = true)",
            nativeQuery = true)
    Page<JobPosting> searchJobPostings(@Param("searchTerm") String searchTerm, @Param("activeOnly") Boolean activeOnly, Pageable pageable);
}
