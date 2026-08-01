// PostMediaRepository.java
package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, String> {

    List<PostMedia> findByPostId(String postId);

    List<PostMedia> findByPostIdOrderByDisplayOrderAsc(String postId);

    List<PostMedia> findByUserId(String userId);

    List<PostMedia> findByMediaType(String mediaType);

    long countByPostId(String postId);

    List<PostMedia> findByPostIdAndSubtitleIn(String postId, List<String> subtitles);
}
