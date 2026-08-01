package in.mapmytour.customer.repository;

import in.mapmytour.customer.entity.TicketConversation;
import in.mapmytour.customer.entity.TicketConversation.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketConversationRepository extends JpaRepository<TicketConversation, String> {

    Page<TicketConversation> findByTicketId(String ticketId, Pageable pageable);

    @Query("SELECT c FROM TicketConversation c WHERE " +
            "c.ticketId = :ticketId AND " +
            "(:includeInternal = true OR c.isInternalNote = false)")
    Page<TicketConversation> findByTicketIdWithInternalFilter(
            @Param("ticketId") String ticketId,
            @Param("includeInternal") boolean includeInternal,
            Pageable pageable);

    List<TicketConversation> findByTicketIdOrderBySentAtAsc(String ticketId);

    @Query("SELECT c FROM TicketConversation c WHERE " +
            "c.ticketId = :ticketId AND " +
            "c.isInternalNote = false " +
            "ORDER BY c.sentAt DESC")
    List<TicketConversation> findPublicConversationsByTicketId(@Param("ticketId") String ticketId);

    long countByTicketId(String ticketId);

    @Query("SELECT c FROM TicketConversation c WHERE " +
            "c.senderId = :userId")
    Page<TicketConversation> findBySenderId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM TicketConversation c WHERE " +
            "c.ticketId = :ticketId AND " +
            "c.senderType = :senderType")
    long countByTicketIdAndSenderType(
            @Param("ticketId") String ticketId,
            @Param("senderType") SenderType senderType);

    @Query("SELECT c FROM TicketConversation c WHERE " +
            "c.ticketId = :ticketId AND " +
            "c.senderType = :senderType")
    List<TicketConversation> findByTicketIdAndSenderType(
            @Param("ticketId") String ticketId,
            @Param("senderType") SenderType senderType);

    @Query("SELECT DISTINCT c.ticketId FROM TicketConversation c WHERE c.senderId = :senderId")
    List<String> findDistinctTicketIdsBySenderId(@Param("senderId") String senderId);
}