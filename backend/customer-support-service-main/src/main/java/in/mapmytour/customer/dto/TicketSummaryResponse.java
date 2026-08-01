package in.mapmytour.customer.dto;

import in.mapmytour.customer.entity.SupportTicket;
import lombok.*;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryResponse {

    private String id;
    private String subject;
    private SupportTicket.TicketStatus status;
    private SupportTicket.TicketPriority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}