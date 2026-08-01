package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.TravelGroup;
import in.mapmytour.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TravelGroupRepository extends JpaRepository<TravelGroup, String> {
    List<TravelGroup> findByCreatedByOrderByTravelDateAsc(User user);
    List<TravelGroup> findByDestinationIgnoreCaseAndStatusAndTravelDateGreaterThanEqual(
            String destination, String status, LocalDate travelDate);
    Page<TravelGroup> findByIsPublicTrueAndStatusOrderByTravelDateAsc(String status, Pageable pageable);
    Optional<TravelGroup> findByInviteCode(String inviteCode);
    List<TravelGroup> findByDestinationIgnoreCaseAndStatus(String destination, String status);
}

