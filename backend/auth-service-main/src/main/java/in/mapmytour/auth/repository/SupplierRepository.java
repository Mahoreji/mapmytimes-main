package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.Supplier;
import in.mapmytour.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

        Optional<Supplier> findByUser(User user);

        Optional<Supplier> findBySupplierCode(String supplierCode);

        boolean existsByUser(User user);

        // --- Admin analytics queries ---

        long countByUserIsActive(boolean isActive);

        long countByUserIsVerified(boolean isVerified);

        @Query("SELECT COUNT(s) FROM Supplier s WHERE s.gstin IS NOT NULL AND s.gstin <> ''")
        long countSuppliersWithGstin();

        @Query("SELECT COUNT(s) FROM Supplier s WHERE s.pan IS NOT NULL AND s.pan <> ''")
        long countSuppliersWithPan();

        @Query("SELECT s.city, COUNT(s) FROM Supplier s GROUP BY s.city ORDER BY COUNT(s) DESC")
        List<Object[]> countByCity();

        @Query("SELECT s.state, COUNT(s) FROM Supplier s GROUP BY s.state ORDER BY COUNT(s) DESC")
        List<Object[]> countByState();

        @Query("SELECT s.country, COUNT(s) FROM Supplier s GROUP BY s.country ORDER BY COUNT(s) DESC")
        List<Object[]> countByCountry();

        @Query("SELECT s.supplierType, COUNT(s) FROM Supplier s WHERE s.supplierType IS NOT NULL GROUP BY s.supplierType ORDER BY COUNT(s) DESC")
        List<Object[]> countBySupplierType();

        @Query("SELECT s.businessCategory, COUNT(s) FROM Supplier s WHERE s.businessCategory IS NOT NULL GROUP BY s.businessCategory ORDER BY COUNT(s) DESC")
        List<Object[]> countByBusinessCategory();

        @Query("SELECT TO_CHAR(s.createdAt, 'YYYY-MM'), COUNT(s) FROM Supplier s GROUP BY TO_CHAR(s.createdAt, 'YYYY-MM') ORDER BY 1 ASC")
        List<Object[]> countRegistrationsByMonth();

        // --- Paginated list for admin ---
        @Query(value = "SELECT s FROM Supplier s JOIN FETCH s.user WHERE " +
                        "(:active IS NULL OR s.user.isActive = :active) AND " +
                        "(:verified IS NULL OR s.user.isVerified = :verified) AND " +
                        "(:city IS NULL OR LOWER(CAST(s.city AS string)) LIKE :city) AND " +
                        "(:supplierType IS NULL OR LOWER(CAST(s.supplierType AS string)) LIKE :supplierType) AND "
                        +
                        "(:search IS NULL OR " +
                        "LOWER(CAST(s.companyName AS string)) LIKE :search OR " +
                        "LOWER(CAST(s.supplierCode AS string)) LIKE :search OR " +
                        "LOWER(CAST(s.contactPerson AS string)) LIKE :search OR " +
                        "LOWER(CAST(s.user.email AS string)) LIKE :search)", countQuery = "SELECT COUNT(s) FROM Supplier s JOIN s.user WHERE "
                                        +
                                        "(:active IS NULL OR s.user.isActive = :active) AND " +
                                        "(:verified IS NULL OR s.user.isVerified = :verified) AND " +
                                        "(:city IS NULL OR LOWER(CAST(s.city AS string)) LIKE :city) AND "
                                        +
                                        "(:supplierType IS NULL OR LOWER(CAST(s.supplierType AS string)) LIKE :supplierType) AND "
                                        +
                                        "(:search IS NULL OR " +
                                        "LOWER(CAST(s.companyName AS string)) LIKE :search OR "
                                        +
                                        "LOWER(CAST(s.supplierCode AS string)) LIKE :search OR "
                                        +
                                        "LOWER(CAST(s.contactPerson AS string)) LIKE :search OR "
                                        +
                                        "LOWER(CAST(s.user.email AS string)) LIKE :search)")
        Page<Supplier> findAllWithFilters(
                        @Param("active") Boolean active,
                        @Param("verified") Boolean verified,
                        @Param("city") String city,
                        @Param("supplierType") String supplierType,
                        @Param("search") String search,
                        Pageable pageable);
}
