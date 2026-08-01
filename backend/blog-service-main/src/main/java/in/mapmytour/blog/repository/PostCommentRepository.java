// Updated PostCommentRepository.java
package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, String> {

    List<PostComment> findByPostId(String postId);

    Page<PostComment> findByPostId(String postId, Pageable pageable);

    Page<PostComment> findByPostIdAndStatus(String postId, String status, Pageable pageable);

    List<PostComment> findByParentCommentId(String parentCommentId);

    @Query("SELECT pc FROM PostComment pc WHERE pc.userId = :userId ORDER BY pc.createdAt DESC")
    Page<PostComment> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT pc FROM PostComment pc WHERE pc.status = :status ORDER BY pc.createdAt DESC")
    Page<PostComment> findByStatus(@Param("status") String status, Pageable pageable);

    long countByPostId(String postId);

    long countByPostIdAndStatus(String postId, String status);

    long countByStatus(String status);

    @Query("SELECT pc FROM PostComment pc WHERE pc.post.id = :postId AND pc.parentCommentId IS NULL ORDER BY pc.createdAt DESC")
    Page<PostComment> findTopLevelCommentsByPostId(@Param("postId") String postId, Pageable pageable);

    @Query("SELECT pc FROM PostComment pc WHERE pc.parentCommentId = :parentId ORDER BY pc.createdAt ASC")
    List<PostComment> findRepliesByParentId(@Param("parentId") String parentId);
}
