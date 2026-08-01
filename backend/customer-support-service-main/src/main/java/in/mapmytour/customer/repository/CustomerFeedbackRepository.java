package in.mapmytour.customer.repository;

import in.mapmytour.customer.entity.CustomerFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, String> {

    Page<CustomerFeedback> findByCustomerId(String customerId, Pageable pageable);

    Page<CustomerFeedback> findByTicketId(String ticketId, Pageable pageable);

    @Query("SELECT f FROM CustomerFeedback f WHERE " +
            "(:minRating IS NULL OR f.rating >= :minRating) AND " +
            "(:maxRating IS NULL OR f.rating <= :maxRating)")
    Page<CustomerFeedback> findByRatingRange(
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating,
            Pageable pageable);

    @Query("SELECT f FROM CustomerFeedback f WHERE " +
            "f.rating >= 4 AND " +
            "f.submittedAt BETWEEN :start AND :end")
    List<CustomerFeedback> findPositiveFeedbackInPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT AVG(f.rating) FROM CustomerFeedback f")
    Double findAverageRating();

    @Query("SELECT COUNT(f) FROM CustomerFeedback f WHERE f.rating >= :rating")
    Long countByRatingGreaterThanEqual(@Param("rating") Integer rating);

    @Query("SELECT COUNT(f) FROM CustomerFeedback f WHERE f.rating = :rating")
    Long countByRating(@Param("rating") Integer rating);

    @Query("SELECT COUNT(f) FROM CustomerFeedback f WHERE " +
            "f.isFollowUpRequired = true AND " +
            "f.submittedAt BETWEEN :start AND :end")
    Long countFeedbacksRequiringFollowUp(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    boolean existsByTicketId(String ticketId);

    @Query("SELECT AVG(f.rating) FROM CustomerFeedback f WHERE f.ticketId IN :ticketIds")
    Double findAverageRatingByTicketIdIn(@Param("ticketIds") List<String> ticketIds);

    @Query("SELECT COUNT(f) FROM CustomerFeedback f WHERE f.ticketId IN :ticketIds")
    Long countByTicketIdIn(@Param("ticketIds") List<String> ticketIds);

    @Query("SELECT COUNT(f) FROM CustomerFeedback f WHERE " +
            "f.submittedAt BETWEEN :start AND :end")
    Long countBySubmittedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}