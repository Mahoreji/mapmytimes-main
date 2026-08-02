package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, String> {

    Optional<Section> findByName(String name);

    Optional<Section> findBySlug(String slug);

    List<Section> findByParentSectionId(String parentSectionId);

    List<Section> findByParentSectionIdIsNull();

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    @Query("SELECT s, " +
            "(SELECT COUNT(bp) FROM BlogPost bp WHERE bp.sectionSlug = s.slug) as postCount " +
            "FROM Section s WHERE s.id = :sectionId")
    Object[] findSectionWithPostCount(@Param("sectionId") String sectionId);

    @Query("SELECT s.id, s.name, " +
            "(SELECT COUNT(bp) FROM BlogPost bp WHERE bp.sectionSlug = s.slug) as postCount " +
            "FROM Section s")
    List<Object[]> findAllSectionsWithPostCount();
}
