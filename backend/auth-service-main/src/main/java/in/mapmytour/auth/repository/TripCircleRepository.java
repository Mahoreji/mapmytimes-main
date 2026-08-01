package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.CircleStatus;
import in.mapmytour.auth.entity.TripCircle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripCircleRepository extends JpaRepository<TripCircle, String> {

    List<TripCircle> findByCreatedByUserIdOrderByStartDateAsc(String userId);

    List<TripCircle> findByDestinationIdAndStatusAndEndDateGreaterThanEqual(
            String destinationId, CircleStatus status, LocalDate date);

    List<TripCircle> findByDestinationIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String destinationId, CircleStatus status, LocalDate from, LocalDate to);
}
