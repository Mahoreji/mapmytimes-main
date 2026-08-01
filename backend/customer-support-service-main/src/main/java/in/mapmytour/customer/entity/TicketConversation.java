package in.mapmytour.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String ticketId;

    @Column(nullable = false)
    private String senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column
    private String attachmentUrl;

    @CreationTimestamp
    private LocalDateTime sentAt;

    @Column
    private boolean isInternalNote;

    public enum SenderType {
        CUSTOMER, SUPPORT_AGENT, SYSTEM, AGENT
    }
}