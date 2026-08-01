package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.entity.VerificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, String> {

        /**
         * Find verification request by user
         */
        List<VerificationRequest> findByUser(User user);

        /**
         * Find pending verification requests
         */
        List<VerificationRequest> findByStatus(VerificationRequest.VerificationStatus status);

        /**
         * Find pending verification requests with pagination
         */
        Page<VerificationRequest> findByStatus(VerificationRequest.VerificationStatus status, Pageable pageable);

        /**
         * Find verification request by user and status
         */
        Optional<VerificationRequest> findByUserAndStatus(User user, VerificationRequest.VerificationStatus status);

        /**
         * Find latest verification request by user
         */
        @Query("SELECT vr FROM VerificationRequest vr WHERE vr.user = :user ORDER BY vr.createdAt DESC")
        Optional<VerificationRequest> findLatestByUser(@Param("user") User user);

        /**
         * Count pending verification requests
         */
        long countByStatus(VerificationRequest.VerificationStatus status);

        long countByVerificationTypeAndStatus(String verificationType, VerificationRequest.VerificationStatus status);

        long countByVerificationTypeAndStatusAndReviewedByIsNull(String verificationType,
                        VerificationRequest.VerificationStatus status);

        long countByVerificationTypeAndStatusAndReviewedByIsNotNull(String verificationType,
                        VerificationRequest.VerificationStatus status);

        List<VerificationRequest> findByUserOrderByCreatedAtDesc(User user);
}
