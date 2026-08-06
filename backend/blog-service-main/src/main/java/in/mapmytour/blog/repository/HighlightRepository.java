package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.Highlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HighlightRepository extends JpaRepository<Highlight, String> {

    List<Highlight> findByUserIdAndPostIdOrderByCreatedAtAsc(String userId, String postId);

    void deleteByIdAndUserId(String id, String userId);
}
