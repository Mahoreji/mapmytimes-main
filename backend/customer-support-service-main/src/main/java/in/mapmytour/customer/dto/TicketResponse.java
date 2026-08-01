package in.mapmytour.customer.dto;

import in.mapmytour.customer.entity.SupportTicket;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private String id;
    private String customerId;
    private String subject;
    private String description;
    private SupportTicket.TicketPriority priority;
    private SupportTicket.TicketStatus status;
    private SupportTicket.TicketCategory category;
    private String assignedAgentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    
    // SLA Management Fields
    private LocalDateTime firstResponseAt;
    private Long responseTimeMinutes;
    private Long resolutionTimeMinutes;
    private Integer slaResponseTimeMinutes;
    private Integer slaResolutionTimeMinutes;
    private Boolean slaResponseMet;
    private Boolean slaResolutionMet;
    
    // Booking System Integration
    private String bookingId;
    private String bookingReference;
    
    // Multi-language Support
    private String language;
    
    // Escalation Fields
    private Integer escalationLevel;
    private LocalDateTime escalatedAt;
    private String escalationReason;
}
