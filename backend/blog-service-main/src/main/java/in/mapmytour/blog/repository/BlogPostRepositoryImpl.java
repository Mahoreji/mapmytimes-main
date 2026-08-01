package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.BlogPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class BlogPostRepositoryImpl implements BlogPostRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<BlogPost> searchPostsWithContent(String keyword, String status, String userId, Pageable pageable) {
        try {
            // Build base query with bytea to text conversion
            // Build WHERE clause conditionally to avoid NULL parameter type issues
            StringBuilder queryBuilder = new StringBuilder(
                    "SELECT bp.* FROM blog_posts bp WHERE 1=1"
            );
            
            // Add keyword search condition
            if (keyword != null && !keyword.trim().isEmpty()) {
                queryBuilder.append(" AND (LOWER(bp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(bp.content::text) LIKE LOWER(CONCAT('%', :keyword, '%')))");
            }
            
            // Add status filter
            if (status != null && !status.trim().isEmpty()) {
                queryBuilder.append(" AND bp.status = :status");
            }
            
            // Add userId filter
            if (userId != null && !userId.trim().isEmpty()) {
                queryBuilder.append(" AND bp.user_id = :userId");
            }

            // Build ORDER BY clause from Pageable
            if (pageable.getSort().isSorted()) {
                queryBuilder.append(" ORDER BY ");
                List<String> orderClauses = pageable.getSort().stream()
                        .map(order -> {
                            String property = order.getProperty();
                            // Convert camelCase to snake_case for database columns
                            String columnName = camelToSnakeCase(property);
                            String direction = order.getDirection() == Sort.Direction.ASC ? "ASC" : "DESC";
                            return "bp." + columnName + " " + direction;
                        })
                        .toList();
                queryBuilder.append(String.join(", ", orderClauses));
            } else {
                // Default sorting
                queryBuilder.append(" ORDER BY bp.created_at DESC");
            }

            // Create query
            Query query = entityManager.createNativeQuery(queryBuilder.toString(), BlogPost.class);
            
            // Set parameters only if they are not null
            if (keyword != null && !keyword.trim().isEmpty()) {
                query.setParameter("keyword", keyword);
            }
            if (status != null && !status.trim().isEmpty()) {
                query.setParameter("status", status);
            }
            if (userId != null && !userId.trim().isEmpty()) {
                query.setParameter("userId", userId);
            }

            // Apply pagination
            if (!pageable.isUnpaged()) {
                query.setFirstResult((int) pageable.getOffset());
                query.setMaxResults(pageable.getPageSize());
            }

            @SuppressWarnings("unchecked")
            List<BlogPost> results = query.getResultList();

            // Get total count - build query conditionally
            StringBuilder countQueryBuilder = new StringBuilder("SELECT COUNT(*) FROM blog_posts bp WHERE 1=1");
            if (keyword != null && !keyword.trim().isEmpty()) {
                countQueryBuilder.append(" AND (LOWER(bp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(bp.content::text) LIKE LOWER(CONCAT('%', :keyword, '%')))");
            }
            if (status != null && !status.trim().isEmpty()) {
                countQueryBuilder.append(" AND bp.status = :status");
            }
            if (userId != null && !userId.trim().isEmpty()) {
                countQueryBuilder.append(" AND bp.user_id = :userId");
            }

            Query countQuery = entityManager.createNativeQuery(countQueryBuilder.toString());
            if (keyword != null && !keyword.trim().isEmpty()) {
                countQuery.setParameter("keyword", keyword);
            }
            if (status != null && !status.trim().isEmpty()) {
                countQuery.setParameter("status", status);
            }
            if (userId != null && !userId.trim().isEmpty()) {
                countQuery.setParameter("userId", userId);
            }
            
            Long total = ((Number) countQuery.getSingleResult()).longValue();

            return new PageImpl<>(results, pageable, total);
        } catch (Exception e) {
            log.error("Error executing search query with content: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search blog posts: " + e.getMessage(), e);
        }
    }

    private String camelToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        // Handle common field mappings
        return switch (camelCase) {
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "publishedAt" -> "published_at";
            case "userId" -> "user_id";
            case "allowComments" -> "allow_comments";
            case "allowLikes" -> "allow_likes";
            default -> {
                // Convert camelCase to snake_case
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < camelCase.length(); i++) {
                    char c = camelCase.charAt(i);
                    if (Character.isUpperCase(c) && i > 0) {
                        result.append('_');
                    }
                    result.append(Character.toLowerCase(c));
                }
                yield result.toString();
            }
        };
    }
}
