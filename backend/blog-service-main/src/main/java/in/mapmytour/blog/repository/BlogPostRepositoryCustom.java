package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BlogPostRepositoryCustom {
    Page<BlogPost> searchPostsWithContent(String keyword, String status, String userId, Pageable pageable);
}
