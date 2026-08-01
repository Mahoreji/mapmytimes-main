package in.mapmytour.customer.repository;

import in.mapmytour.customer.entity.CustomerSupportAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerSupportAgentRepository extends JpaRepository<CustomerSupportAgent, String> {

    Page<CustomerSupportAgent> findByIsActive(boolean isActive, Pageable pageable);

    @Query("SELECT a FROM CustomerSupportAgent a WHERE " +
            ":skill MEMBER OF a.skills")
    List<CustomerSupportAgent> findBySkillsContaining(@Param("skill") String skill);

    @Query("SELECT a FROM CustomerSupportAgent a WHERE " +
            ":skill MEMBER OF a.skills AND " +
            "a.isActive = true")
    List<CustomerSupportAgent> findActiveAgentsWithSkill(@Param("skill") String skill);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM CustomerSupportAgent a WHERE " +
            "LOWER(a.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    boolean existsByUserId(String userId);

    @Query("SELECT a FROM CustomerSupportAgent a WHERE " +
            "LOWER(a.email) = LOWER(:email)")
    Optional<CustomerSupportAgent> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT a FROM CustomerSupportAgent a WHERE " +
            "a.isActive = true AND " +
            "(a.maxActiveTickets IS NULL OR " +
            "a.maxActiveTickets > (SELECT COUNT(t) FROM SupportTicket t WHERE " +
            "t.assignedAgentId = a.id AND " +
            "t.status NOT IN ('RESOLVED', 'CLOSED')))")
    List<CustomerSupportAgent> findAvailableAgents();

    @Query("SELECT a FROM CustomerSupportAgent a WHERE " +
            "a.isActive = true " +
            "ORDER BY (SELECT COUNT(t) FROM SupportTicket t WHERE " +
            "t.assignedAgentId = a.id AND " +
            "t.status NOT IN ('RESOLVED', 'CLOSED')) ASC")
    List<CustomerSupportAgent> findAgentsOrderByWorkload();

    @Query("SELECT a FROM CustomerSupportAgent a WHERE " +
            "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<CustomerSupportAgent> searchAgents(@Param("searchTerm") String searchTerm, Pageable pageable);

    long countByIsActive(boolean isActive);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE " +
            "t.assignedAgentId = :agentId AND " +
            "t.status NOT IN ('RESOLVED', 'CLOSED')")
    long countActiveTicketsByAgent(@Param("agentId") String agentId);
}