package in.mapmytour.customer.dto;

import in.mapmytour.customer.entity.TicketConversation.SenderType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String id;
    private String ticketId;
    private String senderId;
    private SenderType senderType;
    private String message;
    private String attachmentUrl;
    private boolean isInternalNote;
    private LocalDateTime sentAt;
}