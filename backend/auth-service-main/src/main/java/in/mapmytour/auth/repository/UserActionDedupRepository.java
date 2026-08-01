package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.UserActionDedup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserActionDedupRepository extends JpaRepository<UserActionDedup, String> {

    Optional<UserActionDedup> findByUserIdAndCircleIdAndActionTypeAndActionDate(
            String userId, String circleId, String actionType, LocalDate actionDate);
}
