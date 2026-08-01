package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, String> {

    Page<UserActivity> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<UserActivity> findAllByOrderByCreatedAtDesc(Pageable pageable); // For admin access
}

