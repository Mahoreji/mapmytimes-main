package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);
    long countByRecipientEmailAndIsReadFalse(String recipientEmail);
}
