// CategoryRepository.java
package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    Optional<Category> findByName(String name);

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentCategoryId(String parentCategoryId);

    List<Category> findByParentCategoryIdIsNull();

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    @Query("SELECT c, " +
            "(SELECT COUNT(bp) FROM BlogPost bp WHERE :categoryId MEMBER OF bp.categories) as postCount " +
            "FROM Category c WHERE c.id = :categoryId")
    Object[] findCategoryWithPostCount(@Param("categoryId") String categoryId);

    @Query("SELECT c.id, c.name, " +
            "(SELECT COUNT(bp) FROM BlogPost bp WHERE c.id MEMBER OF bp.categories) as postCount " +
            "FROM Category c")
    List<Object[]> findAllCategoriesWithPostCount();
}