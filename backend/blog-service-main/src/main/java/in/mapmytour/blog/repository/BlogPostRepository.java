// BlogPostRepository.java
package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, String>, BlogPostRepositoryCustom {

    Optional<BlogPost> findBySlug(String slug);

    List<BlogPost> findByUserId(String userId);

    List<BlogPost> findByStatus(String status);

    Page<BlogPost> findByStatus(String status, Pageable pageable);

    Page<BlogPost> findByUserId(String userId, Pageable pageable);

    Page<BlogPost> findByStatusAndUserId(String status, String userId, Pageable pageable);
    
    Page<BlogPost> findByPostType(String postType, Pageable pageable);
    Page<BlogPost> findByStatusAndPostType(String status, String postType, Pageable pageable);
    Page<BlogPost> findByStatusAndPostTypeIn(String status, List<String> postTypes, Pageable pageable);
    Page<BlogPost> findByUserIdInAndStatusAndPostTypeIn(List<String> userIds, String status, List<String> postTypes, Pageable pageable);

    Page<BlogPost> findByStatusAndLanguage(String status, String language, Pageable pageable);
    Page<BlogPost> findByStatusAndPostTypeAndLanguage(String status, String postType, String language, Pageable pageable);
    Page<BlogPost> findByStatusAndPostTypeInAndLanguage(String status, List<String> postTypes, String language, Pageable pageable);
    Page<BlogPost> findByUserIdInAndStatusAndPostTypeInAndLanguage(List<String> userIds, String status, List<String> postTypes, String language, Pageable pageable);
    List<BlogPost> findByStatusAndLanguage(String status, String language);
    
    @Query("SELECT bp FROM BlogPost bp WHERE bp.status = 'PUBLISHED' OR bp.userId = :userId")
    Page<BlogPost> findPublishedOrUserPosts(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.status = 'PUBLISHED' ORDER BY SIZE(bp.likes) DESC, SIZE(bp.comments) DESC")
    List<BlogPost> findPopularPosts(Pageable pageable);

    // Default implementation searches only on title (for backward compatibility)
    // Use searchPostsWithContent for full-text search including content
    @Query("SELECT bp FROM BlogPost bp WHERE " +
            "(:keyword IS NULL OR LOWER(bp.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR bp.status = :status) AND " +
            "(:userId IS NULL OR bp.userId = :userId)")
    Page<BlogPost> searchPosts(@Param("keyword") String keyword,
                               @Param("status") String status,
                               @Param("userId") String userId,
                               Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp WHERE :category MEMBER OF bp.categories")
    Page<BlogPost> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp WHERE :tag MEMBER OF bp.tags")
    Page<BlogPost> findByTag(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.sectionSlug = :sectionSlug")
    Page<BlogPost> findBySectionSlug(@Param("sectionSlug") String sectionSlug, Pageable pageable);

    Page<BlogPost> findByStatusAndSectionSlug(String status, String sectionSlug, Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp JOIN bp.likes pl WHERE pl.userId = :userId")
    Page<BlogPost> findLikedPostsByUserId(@Param("userId") String userId, Pageable pageable);

    long countByStatus(String status);

    long countByUserId(String userId);

    @Query("SELECT COUNT(bp) FROM BlogPost bp WHERE bp.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    boolean existsBySlug(String slug);

    @Query("SELECT DISTINCT bp FROM BlogPost bp LEFT JOIN FETCH bp.media WHERE bp.id = :id")
    Optional<BlogPost> findByIdWithMedia(@Param("id") String id);

    @Query("SELECT DISTINCT bp FROM BlogPost bp LEFT JOIN FETCH bp.media WHERE bp.slug = :slug")
    Optional<BlogPost> findBySlugWithMedia(@Param("slug") String slug);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE BlogPost bp SET bp.viewCount = bp.viewCount + 1 WHERE bp.id = :id")
    void incrementViewCount(@Param("id") String id);
}
