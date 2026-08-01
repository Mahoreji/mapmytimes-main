package in.mapmytour.customer.repository;

import in.mapmytour.customer.entity.KnowledgeBaseArticle;
import in.mapmytour.customer.entity.KnowledgeBaseArticle.ArticleCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseArticleRepository extends JpaRepository<KnowledgeBaseArticle, String> {

    Page<KnowledgeBaseArticle> findByIsPublished(boolean isPublished, Pageable pageable);

    Page<KnowledgeBaseArticle> findByCategoryAndIsPublished(ArticleCategory category, boolean isPublished, Pageable pageable);

    Page<KnowledgeBaseArticle> findByCategory(ArticleCategory category, Pageable pageable);

    @Query("SELECT a FROM KnowledgeBaseArticle a WHERE " +
            "a.isPublished = :published AND (" +
            "LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "EXISTS (SELECT k FROM a.keywords k WHERE LOWER(k) LIKE LOWER(CONCAT('%', :query, '%'))))")
    Page<KnowledgeBaseArticle> searchPublishedArticles(
            @Param("query") String query,
            @Param("published") boolean published,
            Pageable pageable);

    @Query("SELECT a FROM KnowledgeBaseArticle a WHERE " +
            "a.isPublished = true " +
            "ORDER BY a.viewCount DESC")
    Page<KnowledgeBaseArticle> findPopularArticles(Pageable pageable);

    @Query("SELECT DISTINCT a.category FROM KnowledgeBaseArticle a WHERE a.isPublished = true")
    List<ArticleCategory> findAllPublishedCategories();

    long countByIsPublished(boolean isPublished);

    @Modifying
    @Query("UPDATE KnowledgeBaseArticle a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") String id);

    Optional<KnowledgeBaseArticle> findByIdAndIsPublished(String id, boolean isPublished);

    boolean existsByTitleIgnoreCase(String title);

    @Query("SELECT a FROM KnowledgeBaseArticle a WHERE " +
            "LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<KnowledgeBaseArticle> searchByTitleOrContent(@Param("searchTerm") String searchTerm, Pageable pageable);
}