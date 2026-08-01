package in.mapmytour.blog.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMaintenanceService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void purgeAllBlogPosts() {
        entityManager.createNativeQuery("TRUNCATE TABLE blog_posts CASCADE").executeUpdate();
    }
}

