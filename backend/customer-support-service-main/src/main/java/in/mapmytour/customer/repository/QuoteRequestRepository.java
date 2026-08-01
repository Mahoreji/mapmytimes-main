package in.mapmytour.customer.repository;

import in.mapmytour.customer.entity.QuoteRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, String> {

    Page<QuoteRequest> findByStatus(String status, Pageable pageable);

    @Query("SELECT q FROM QuoteRequest q WHERE " +
            "LOWER(q.personalInfo.email) = LOWER(:email)")
    Page<QuoteRequest> findByPersonalInfoEmailIgnoreCase(@Param("email") String email, Pageable pageable);

    @Query("SELECT q FROM QuoteRequest q WHERE " +
            "LOWER(q.tripDetails.destination) LIKE LOWER(CONCAT('%', :destination, '%'))")
    Page<QuoteRequest> findByTripDetailsDestinationContainingIgnoreCase(
            @Param("destination") String destination, Pageable pageable);

    Page<QuoteRequest> findByAssignedAgentId(String agentId, Pageable pageable);

    @Query("SELECT q FROM QuoteRequest q WHERE " +
            "q.tripDetails.departureDate BETWEEN :startDate AND :endDate")
    List<QuoteRequest> findQuotesBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT q FROM QuoteRequest q WHERE " +
            "LOWER(q.status) = 'pending' AND " +
            "(q.assignedAgentId IS NULL OR q.assignedAgentId = '')")
    List<QuoteRequest> findUnassignedPendingQuotes();

    long countByStatus(String status);

    @Query("SELECT COUNT(q) FROM QuoteRequest q WHERE " +
            "LOWER(q.personalInfo.email) = LOWER(:email)")
    long countByPersonalInfoEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT q FROM QuoteRequest q WHERE " +
            "LOWER(q.personalInfo.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(q.personalInfo.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(q.tripDetails.destination) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(q.tripDetails.departureCity) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<QuoteRequest> searchQuotes(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QuoteRequest q WHERE " +
            "q.assignedAgentId = :agentId AND " +
            "LOWER(q.status) NOT IN ('completed', 'rejected')")
    long countActiveQuotesByAgent(@Param("agentId") String agentId);
}