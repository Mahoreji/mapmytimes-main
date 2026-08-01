package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.Agent;
import in.mapmytour.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {

        Optional<Agent> findByUser(User user);

        Optional<Agent> findByAgentCode(String agentCode);

        boolean existsByUser(User user);

        // --- Admin analytics queries ---

        long countByUserIsActive(boolean isActive);

        long countByUserIsVerified(boolean isVerified);

        @Query("SELECT COUNT(a) FROM Agent a WHERE a.gstin IS NOT NULL AND a.gstin <> ''")
        long countAgentsWithGstin();

        @Query("SELECT COUNT(a) FROM Agent a WHERE a.pan IS NOT NULL AND a.pan <> ''")
        long countAgentsWithPan();

        @Query("SELECT a.city, COUNT(a) FROM Agent a GROUP BY a.city ORDER BY COUNT(a) DESC")
        List<Object[]> countByCity();

        @Query("SELECT a.state, COUNT(a) FROM Agent a GROUP BY a.state ORDER BY COUNT(a) DESC")
        List<Object[]> countByState();

        @Query("SELECT a.country, COUNT(a) FROM Agent a GROUP BY a.country ORDER BY COUNT(a) DESC")
        List<Object[]> countByCountry();

        @Query("SELECT a.businessType, COUNT(a) FROM Agent a WHERE a.businessType IS NOT NULL GROUP BY a.businessType ORDER BY COUNT(a) DESC")
        List<Object[]> countByBusinessType();

        @Query("SELECT a.businessCategory, COUNT(a) FROM Agent a WHERE a.businessCategory IS NOT NULL GROUP BY a.businessCategory ORDER BY COUNT(a) DESC")
        List<Object[]> countByBusinessCategory();

        @Query("SELECT TO_CHAR(a.createdAt, 'YYYY-MM'), COUNT(a) FROM Agent a GROUP BY TO_CHAR(a.createdAt, 'YYYY-MM') ORDER BY 1 ASC")
        List<Object[]> countRegistrationsByMonth();

        // --- Paginated list for admin ---
        @Query(value = "SELECT a FROM Agent a JOIN FETCH a.user WHERE " +
                        "(:active IS NULL OR a.user.isActive = :active) AND " +
                        "(:verified IS NULL OR a.user.isVerified = :verified) AND " +
                        "(:city IS NULL OR LOWER(CAST(a.city AS string)) LIKE :city) AND " +
                        "(:state IS NULL OR LOWER(CAST(a.state AS string)) LIKE :state) AND " +
                        "(:search IS NULL OR " +
                        "LOWER(CAST(a.agencyName AS string)) LIKE :search OR " +
                        "LOWER(CAST(a.agentCode AS string)) LIKE :search OR " +
                        "LOWER(CAST(a.contactPerson AS string)) LIKE :search OR " +
                        "LOWER(CAST(a.user.email AS string)) LIKE :search)", countQuery = "SELECT COUNT(a) FROM Agent a JOIN a.user WHERE "
                                        +
                                        "(:active IS NULL OR a.user.isActive = :active) AND " +
                                        "(:verified IS NULL OR a.user.isVerified = :verified) AND " +
                                        "(:city IS NULL OR LOWER(CAST(a.city AS string)) LIKE :city) AND "
                                        +
                                        "(:state IS NULL OR LOWER(CAST(a.state AS string)) LIKE :state) AND "
                                        +
                                        "(:search IS NULL OR " +
                                        "LOWER(CAST(a.agencyName AS string)) LIKE :search OR "
                                        +
                                        "LOWER(CAST(a.agentCode AS string)) LIKE :search OR " +
                                        "LOWER(CAST(a.contactPerson AS string)) LIKE :search OR "
                                        +
                                        "LOWER(CAST(a.user.email AS string)) LIKE :search)")
        Page<Agent> findAllWithFilters(
                        @Param("active") Boolean active,
                        @Param("verified") Boolean verified,
                        @Param("city") String city,
                        @Param("state") String state,
                        @Param("search") String search,
                        Pageable pageable);
}
