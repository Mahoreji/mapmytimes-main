package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.SocialNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialNotificationRepository extends JpaRepository<SocialNotification, String> {
    List<SocialNotification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
}
