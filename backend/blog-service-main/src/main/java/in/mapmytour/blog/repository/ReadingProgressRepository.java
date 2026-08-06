package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.ReadingProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, String> {

    Optional<ReadingProgress> findByUserIdAndPostId(String userId, String postId);

    List<ReadingProgress> findTopByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);

    void deleteByUserIdAndPostId(String userId, String postId);
}
