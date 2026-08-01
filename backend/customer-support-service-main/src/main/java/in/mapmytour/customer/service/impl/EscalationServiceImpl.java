package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.entity.SupportTicket;
import in.mapmytour.customer.service.EscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationServiceImpl implements EscalationService {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private in.mapmytour.customer.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Autowired
    private in.mapmytour.customer.repository.CustomerSupportAgentRepository agentRepository;

    // Escalation time thresholds in minutes
    private static final int CRITICAL_ESCALATION_TIME = 30; // Escalate after 30 minutes
    private static final int HIGH_ESCALATION_TIME = 120; // Escalate after 2 hours
    private static final int MEDIUM_ESCALATION_TIME = 480; // Escalate after 8 hours
    private static final int LOW_ESCALATION_TIME = 1440; // Escalate after 24 hours

    @Override
    public void checkAndEscalate(SupportTicket ticket) {
        if (shouldEscalateByTime(ticket) || shouldEscalateByPriority(ticket)) {
            String reason = getEscalationReason(ticket);
            escalateTicket(ticket, reason);
        }
    }

    @Override
    public void escalateTicket(SupportTicket ticket, String reason) {
        log.info("Escalating ticket {} to level {}", ticket.getId(), 
                (ticket.getEscalationLevel() == null ? 0 : ticket.getEscalationLevel()) + 1);
        
        int currentLevel = ticket.getEscalationLevel() == null ? 0 : ticket.getEscalationLevel();
        ticket.setEscalationLevel(currentLevel + 1);
        ticket.setEscalatedAt(LocalDateTime.now());
        ticket.setEscalationReason(reason);
        
        // Increase priority if not already at maximum
        if (ticket.getPriority() != SupportTicket.TicketPriority.CRITICAL) {
            switch (ticket.getPriority()) {
                case LOW:
                    ticket.setPriority(SupportTicket.TicketPriority.MEDIUM);
                    break;
                case MEDIUM:
                    ticket.setPriority(SupportTicket.TicketPriority.HIGH);
                    break;
                case HIGH:
                    ticket.setPriority(SupportTicket.TicketPriority.CRITICAL);
                    break;
                default:
                    break;
            }
        }

        // Send escalation alert
        if (notificationService != null && ticket.getAssignedAgentId() != null && !ticket.getAssignedAgentId().isEmpty()) {
            try {
                agentRepository.findById(ticket.getAssignedAgentId()).ifPresent(agent -> {
                    notificationService.sendEscalationNotification(
                        ticket.getId(),
                        agent.getId(),
                        agent.getEmail(),
                        reason
                    );
                });
            } catch (Exception ex) {
                log.warn("Failed to send escalation notification for ticket {}: {}", ticket.getId(), ex.getMessage());
            }
        }
    }

    @Override
    public boolean shouldEscalateByTime(SupportTicket ticket) {
        if (ticket.getStatus() == SupportTicket.TicketStatus.RESOLVED || 
            ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
            return false;
        }
        
        LocalDateTime checkTime = ticket.getUpdatedAt() != null ? 
                ticket.getUpdatedAt() : ticket.getCreatedAt();
        Duration duration = Duration.between(checkTime, LocalDateTime.now());
        long minutesSinceUpdate = duration.toMinutes();
        
        int escalationThreshold;
        switch (ticket.getPriority()) {
            case CRITICAL:
                escalationThreshold = CRITICAL_ESCALATION_TIME;
                break;
            case HIGH:
                escalationThreshold = HIGH_ESCALATION_TIME;
                break;
            case MEDIUM:
                escalationThreshold = MEDIUM_ESCALATION_TIME;
                break;
            case LOW:
                escalationThreshold = LOW_ESCALATION_TIME;
                break;
            default:
                escalationThreshold = MEDIUM_ESCALATION_TIME;
        }
        
        return minutesSinceUpdate > escalationThreshold;
    }

    @Override
    public boolean shouldEscalateByPriority(SupportTicket ticket) {
        // Escalate if CRITICAL priority and no agent assigned after 15 minutes
        if (ticket.getPriority() == SupportTicket.TicketPriority.CRITICAL) {
            if (ticket.getAssignedAgentId() == null || ticket.getAssignedAgentId().isEmpty()) {
                Duration duration = Duration.between(ticket.getCreatedAt(), LocalDateTime.now());
                return duration.toMinutes() > 15;
            }
        }
        return false;
    }

    @Override
    public String getEscalationReason(SupportTicket ticket) {
        if (shouldEscalateByTime(ticket)) {
            Duration duration = Duration.between(
                    ticket.getUpdatedAt() != null ? ticket.getUpdatedAt() : ticket.getCreatedAt(),
                    LocalDateTime.now());
            long hours = duration.toHours();
            return String.format("Ticket inactive for %d hours", hours);
        }
        
        if (shouldEscalateByPriority(ticket)) {
            return "Critical ticket unassigned for more than 15 minutes";
        }
        
        return "Manual escalation";
    }
}

