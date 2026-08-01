package in.mapmytour.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketCategory category;

    @Column
    private String assignedAgentId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private String resolutionNotes;

    // SLA Management Fields
    @Column
    private LocalDateTime firstResponseAt;
    
    @Column
    private Long responseTimeMinutes; // Time to first response in minutes
    
    @Column
    private Long resolutionTimeMinutes; // Time to resolution in minutes
    
    @Column
    private Integer slaResponseTimeMinutes; // SLA target for response time
    
    @Column
    private Integer slaResolutionTimeMinutes; // SLA target for resolution time
    
    @Column
    private Boolean slaResponseMet; // Whether response SLA was met
    
    @Column
    private Boolean slaResolutionMet; // Whether resolution SLA was met
    
    // Booking System Integration
    @Column
    private String bookingId; // Link to booking if ticket is related to a booking
    
    @Column
    private String bookingReference; // Booking reference number
    
    // Multi-language Support
    @Column
    private String language; // Language code (en, es, fr, etc.)
    
    // Escalation Fields
    @Column
    private Integer escalationLevel; // Current escalation level (0 = none, 1-5 = escalated)
    
    @Column
    private LocalDateTime escalatedAt; // When ticket was last escalated
    
    @Column
    private String escalationReason; // Reason for escalation

    public enum TicketPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum TicketStatus {
        OPEN, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED
    }

    public enum TicketCategory {
        TECHNICAL, BILLING, ACCOUNT, GENERAL, FEEDBACK
    }
}