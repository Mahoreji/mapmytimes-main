package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.TravelGroup;
import in.mapmytour.auth.entity.TravelItinerary;
import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TravelItineraryRepository extends JpaRepository<TravelItinerary, String> {
    List<TravelItinerary> findByUserOrderByItineraryDateAscStartTimeAsc(User user);
    List<TravelItinerary> findByUserAndItineraryDateOrderByOrderIndexAsc(User user, LocalDate date);
    List<TravelItinerary> findByGroupOrderByItineraryDateAscStartTimeAsc(TravelGroup group);
    List<TravelItinerary> findByGroupAndItineraryDateOrderByOrderIndexAsc(TravelGroup group, LocalDate date);
    List<TravelItinerary> findByUserAndItineraryDateBetweenOrderByItineraryDateAscStartTimeAsc(
            User user, LocalDate startDate, LocalDate endDate);
}

