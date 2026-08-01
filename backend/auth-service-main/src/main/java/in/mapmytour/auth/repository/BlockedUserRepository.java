package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.BlockedUser;
import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, String> {

    // Find all users blocked by a specific user with eager loading (only active)
    @Query("SELECT bu FROM BlockedUser bu JOIN FETCH bu.blocked WHERE bu.blocker = :blocker AND bu.isActive = true")
    List<BlockedUser> findByBlockerAndIsActiveTrue(@Param("blocker") User blocker);

    // Find all users who blocked a specific user (only active)
    List<BlockedUser> findByBlockedAndIsActiveTrue(User blocked);

    // Find blocking relationship regardless of active flag (for idempotent block/unblock)
    Optional<BlockedUser> findByBlockerAndBlocked(User blocker, User blocked);
}
