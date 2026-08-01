package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.DirectMessage;
import in.mapmytour.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, String> {
    Page<DirectMessage> findBySenderAndRecipientOrderByCreatedAtDesc(
            User sender, User recipient, Pageable pageable);
    Page<DirectMessage> findByRecipientAndSenderOrderByCreatedAtDesc(
            User recipient, User sender, Pageable pageable);
    
    @Query("SELECT dm FROM DirectMessage dm WHERE " +
           "(dm.sender = :user1 AND dm.recipient = :user2) OR " +
           "(dm.sender = :user2 AND dm.recipient = :user1) " +
           "ORDER BY dm.createdAt DESC")
    Page<DirectMessage> findConversationBetweenUsers(
            @Param("user1") User user1, @Param("user2") User user2, Pageable pageable);
    
    /**
     * Find distinct users who have had a conversation with the given user.
     *
     * NOTE:
     *  - The old implementation used a JPQL CASE expression returning an entity.
     *    That works in older Hibernate versions but breaks in Hibernate 6 with:
     *    "SingleTableEntityPersister cannot be cast to BasicValuedMapping".
     *  - We now split this into two simpler queries and combine the results in service code.
     */
    @Query("SELECT DISTINCT dm.sender FROM DirectMessage dm WHERE dm.recipient = :user")
    List<User> findDistinctSendersByRecipient(@Param("user") User user);

    @Query("SELECT DISTINCT dm.recipient FROM DirectMessage dm WHERE dm.sender = :user")
    List<User> findDistinctRecipientsBySender(@Param("user") User user);
    
    @Query("SELECT COUNT(dm) FROM DirectMessage dm WHERE dm.recipient.email = :email AND dm.status = :status")
    long countByRecipientEmailAndStatus(@Param("email") String email, @Param("status") String status);

    long countByRecipientAndStatus(User recipient, String status);
}

