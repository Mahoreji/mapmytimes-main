package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.LoginHistory;
import in.mapmytour.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, String> {

    Page<LoginHistory> findByUserOrderByLoginTimeDesc(User user, Pageable pageable);

    long countByUser(User user);

    // Admin: Get all login history, optionally filtered by user email
    Page<LoginHistory> findAllByOrderByLoginTimeDesc(Pageable pageable);
}

