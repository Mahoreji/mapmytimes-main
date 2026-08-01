// TagRepository.java
package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {

    Optional<Tag> findByName(String name);

    Optional<Tag> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    @Query("SELECT t.id, t.name, " +
            "(SELECT COUNT(bp) FROM BlogPost bp WHERE t.id MEMBER OF bp.tags) as postCount " +
            "FROM Tag t")
    List<Object[]> findAllTagsWithPostCount();

    @Query("SELECT t, " +
            "(SELECT COUNT(bp) FROM BlogPost bp WHERE :tagId MEMBER OF bp.tags) as postCount " +
            "FROM Tag t WHERE t.id = :tagId")
    Object[] findTagWithPostCount(@Param("tagId") String tagId);
}