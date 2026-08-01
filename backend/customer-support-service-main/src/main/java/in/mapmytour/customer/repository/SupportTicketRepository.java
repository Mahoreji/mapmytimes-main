package in.mapmytour.customer.repository;

import in.mapmytour.customer.entity.SupportTicket;
import in.mapmytour.customer.entity.SupportTicket.TicketStatus;
import in.mapmytour.customer.entity.SupportTicket.TicketCategory;
import in.mapmytour.customer.entity.SupportTicket.TicketPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {

    Page<SupportTicket> findByCustomerId(String customerId, Pageable pageable);

    Page<SupportTicket> findByAssignedAgentId(String agentId, Pageable pageable);

    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    Page<SupportTicket> findByCategory(TicketCategory category, Pageable pageable);

    Page<SupportTicket> findByPriority(TicketPriority priority, Pageable pageable);

    @Query(value = "SELECT * FROM support_tickets t WHERE " +
            "(:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(CAST(t.subject AS TEXT)) LIKE LOWER('%' || :searchTerm || '%') OR " +
            "LOWER(CAST(t.description AS TEXT)) LIKE LOWER('%' || :searchTerm || '%') OR " +
            "LOWER(CAST(t.customer_id AS TEXT)) LIKE LOWER('%' || :searchTerm || '%')) " +
            "AND (:status IS NULL OR :status = '' OR t.status = :status) " +
            "AND (:category IS NULL OR :category = '' OR t.category = :category) " +
            "AND (:priority IS NULL OR :priority = '' OR t.priority = :priority)",
            nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM support_tickets t WHERE " +
            "(:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(CAST(t.subject AS TEXT)) LIKE LOWER('%' || :searchTerm || '%') OR " +
            "LOWER(CAST(t.description AS TEXT)) LIKE LOWER('%' || :searchTerm || '%') OR " +
            "LOWER(CAST(t.customer_id AS TEXT)) LIKE LOWER('%' || :searchTerm || '%')) " +
            "AND (:status IS NULL OR :status = '' OR t.status = :status) " +
            "AND (:category IS NULL OR :category = '' OR t.category = :category) " +
            "AND (:priority IS NULL OR :priority = '' OR t.priority = :priority)")
    Page<SupportTicket> searchTickets(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("category") String category,
            @Param("priority") String priority,
            Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE " +
            "t.createdAt BETWEEN :start AND :end")
    List<SupportTicket> findByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByStatus(TicketStatus status);

    long countByCustomerId(String customerId);

    long countByAssignedAgentId(String agentId);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE " +
            "t.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<TicketStatus> statuses);

    @Query("SELECT t FROM SupportTicket t WHERE " +
            "t.status = 'OPEN' AND " +
            "(t.assignedAgentId IS NULL OR t.assignedAgentId = '')")
    List<SupportTicket> findUnassignedOpenTickets();

    @Query("SELECT t FROM SupportTicket t WHERE " +
            "t.assignedAgentId = :agentId AND " +
            "t.status NOT IN ('RESOLVED', 'CLOSED')")
    List<SupportTicket> findActiveTicketsByAgent(@Param("agentId") String agentId);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE " +
            "t.assignedAgentId = :agentId AND " +
            "t.status NOT IN ('RESOLVED', 'CLOSED')")
    long countActiveTicketsByAgent(@Param("agentId") String agentId);

    @Query("SELECT t FROM SupportTicket t WHERE t.status NOT IN (in.mapmytour.customer.entity.SupportTicket.TicketStatus.RESOLVED, in.mapmytour.customer.entity.SupportTicket.TicketStatus.CLOSED)")
    List<SupportTicket> findUnresolvedTickets();
}