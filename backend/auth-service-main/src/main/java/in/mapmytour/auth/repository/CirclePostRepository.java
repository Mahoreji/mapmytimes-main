package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.CirclePost;
import in.mapmytour.auth.entity.CirclePostStatus;
import in.mapmytour.auth.entity.TripCircle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CirclePostRepository extends JpaRepository<CirclePost, String> {

    Page<CirclePost> findByCircleAndStatusOrderByCreatedAtDesc(TripCircle circle, CirclePostStatus status, Pageable pageable);
}
