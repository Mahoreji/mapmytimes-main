package in.mapmytour.customer.dto;


import in.mapmytour.customer.entity.SupportTicket;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {

    private String subject;
    private String description;
    private SupportTicket.TicketPriority priority;
    private SupportTicket.TicketCategory category;
    private String assignedAgentId;
    private String resolutionNotes;
}
