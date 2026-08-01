package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.CirclePoll;
import in.mapmytour.auth.entity.TripCircle;
import in.mapmytour.auth.entity.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CirclePollRepository extends JpaRepository<CirclePoll, String> {

    List<CirclePoll> findByCircleAndStatus(TripCircle circle, PollStatus status);
    
    List<CirclePoll> findByCircleOrderByCreatedAtDesc(TripCircle circle);
}
