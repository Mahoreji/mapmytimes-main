// PostLikeRepository.java
package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, String> {

    Optional<PostLike> findByPostIdAndUserId(String postId, String userId);

    List<PostLike> findByPostId(String postId);

    List<PostLike> findByUserId(String userId);

    long countByPostId(String postId);

    boolean existsByPostIdAndUserId(String postId, String userId);

    void deleteByPostIdAndUserId(String postId, String userId);
}