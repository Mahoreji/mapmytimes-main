package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.TravelPlan;
import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, String> {
    List<TravelPlan> findByUserOrderByTravelDateAsc(User user);
    List<TravelPlan> findByDestinationIgnoreCaseAndStatusAndTravelDateGreaterThanEqual(
            String destination, String status, LocalDate travelDate);
    List<TravelPlan> findByDestinationIgnoreCaseAndStatus(
            String destination, String status);
    List<TravelPlan> findByUserAndStatus(User user, String status);
    boolean existsByUserAndDestinationIgnoreCaseAndStatus(User user, String destination, String status);
    
    /**
     * Find travel plans by destination with user eagerly fetched to avoid N+1 queries
     * Note: Address is @Embedded, so it's automatically loaded with User
     */
    @Query("SELECT tp FROM TravelPlan tp JOIN FETCH tp.user u WHERE " +
           "LOWER(tp.destination) = LOWER(:destination) AND tp.status = :status AND tp.travelDate >= :travelDate")
    List<TravelPlan> findByDestinationIgnoreCaseAndStatusAndTravelDateGreaterThanEqualWithUser(
            @Param("destination") String destination, 
            @Param("status") String status, 
            @Param("travelDate") LocalDate travelDate);
    
    /**
     * Find travel plans by destination with user eagerly fetched (without date filter)
     * Note: Address is @Embedded, so it's automatically loaded with User
     */
    @Query("SELECT tp FROM TravelPlan tp JOIN FETCH tp.user u WHERE " +
           "LOWER(tp.destination) = LOWER(:destination) AND tp.status = :status")
    List<TravelPlan> findByDestinationIgnoreCaseAndStatusWithUser(
            @Param("destination") String destination, 
            @Param("status") String status);
}

