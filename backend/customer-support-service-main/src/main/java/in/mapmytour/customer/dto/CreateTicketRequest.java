package in.mapmytour.customer.dto;

import in.mapmytour.customer.entity.SupportTicket.TicketCategory;
import in.mapmytour.customer.entity.SupportTicket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    // Customer ID is set automatically by controller from user context
    // Not required in request - will be set from authenticated user
    private String customerId;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull
    private TicketPriority priority;

    @NotNull
    private TicketCategory category;
    
    // Booking System Integration
    private String bookingId;
    private String bookingReference;
    
    // Multi-language Support
    private String language; // Language code (en, es, fr, etc.)
}
