package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.entity.SupportTicket;
import in.mapmytour.customer.service.SLAManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class SLAManagementServiceImpl implements SLAManagementService {

    // SLA targets in minutes based on priority
    private static final int CRITICAL_RESPONSE_SLA = 15; // 15 minutes
    private static final int CRITICAL_RESOLUTION_SLA = 240; // 4 hours
    private static final int HIGH_RESPONSE_SLA = 60; // 1 hour
    private static final int HIGH_RESOLUTION_SLA = 1440; // 24 hours
    private static final int MEDIUM_RESPONSE_SLA = 240; // 4 hours
    private static final int MEDIUM_RESOLUTION_SLA = 2880; // 48 hours
    private static final int LOW_RESPONSE_SLA = 480; // 8 hours
    private static final int LOW_RESOLUTION_SLA = 4320; // 72 hours

    @Override
    public void updateSLAMetrics(SupportTicket ticket) {
        log.debug("Updating SLA metrics for ticket: {}", ticket.getId());
        
        // Set SLA targets based on priority
        ticket.setSlaResponseTimeMinutes(getSlaResponseTimeMinutes(ticket.getPriority()));
        ticket.setSlaResolutionTimeMinutes(getSlaResolutionTimeMinutes(ticket.getPriority()));
        
        // Calculate response time
        Long responseTime = calculateResponseTime(ticket);
        if (responseTime != null) {
            ticket.setResponseTimeMinutes(responseTime);
            ticket.setSlaResponseMet(isResponseSLAMet(ticket));
        }
        
        // Calculate resolution time if resolved
        if (ticket.getResolvedAt() != null) {
            Long resolutionTime = calculateResolutionTime(ticket);
            if (resolutionTime != null) {
                ticket.setResolutionTimeMinutes(resolutionTime);
                ticket.setSlaResolutionMet(isResolutionSLAMet(ticket));
            }
        }
    }

    @Override
    public void recordFirstResponse(String ticketId) {
        // This will be called when first conversation is added
        // Implementation in TicketConversationService
    }

    @Override
    public Long calculateResponseTime(SupportTicket ticket) {
        if (ticket.getFirstResponseAt() == null || ticket.getCreatedAt() == null) {
            return null;
        }
        
        Duration duration = Duration.between(ticket.getCreatedAt(), ticket.getFirstResponseAt());
        return duration.toMinutes();
    }

    @Override
    public Long calculateResolutionTime(SupportTicket ticket) {
        if (ticket.getResolvedAt() == null || ticket.getCreatedAt() == null) {
            return null;
        }
        
        Duration duration = Duration.between(ticket.getCreatedAt(), ticket.getResolvedAt());
        return duration.toMinutes();
    }

    @Override
    public Integer getSlaResponseTimeMinutes(SupportTicket.TicketPriority priority) {
        switch (priority) {
            case CRITICAL:
                return CRITICAL_RESPONSE_SLA;
            case HIGH:
                return HIGH_RESPONSE_SLA;
            case MEDIUM:
                return MEDIUM_RESPONSE_SLA;
            case LOW:
                return LOW_RESPONSE_SLA;
            default:
                return MEDIUM_RESPONSE_SLA;
        }
    }

    @Override
    public Integer getSlaResolutionTimeMinutes(SupportTicket.TicketPriority priority) {
        switch (priority) {
            case CRITICAL:
                return CRITICAL_RESOLUTION_SLA;
            case HIGH:
                return HIGH_RESOLUTION_SLA;
            case MEDIUM:
                return MEDIUM_RESOLUTION_SLA;
            case LOW:
                return LOW_RESOLUTION_SLA;
            default:
                return MEDIUM_RESOLUTION_SLA;
        }
    }

    @Override
    public boolean isResponseSLAMet(SupportTicket ticket) {
        if (ticket.getResponseTimeMinutes() == null || ticket.getSlaResponseTimeMinutes() == null) {
            return false;
        }
        return ticket.getResponseTimeMinutes() <= ticket.getSlaResponseTimeMinutes();
    }

    @Override
    public boolean isResolutionSLAMet(SupportTicket ticket) {
        if (ticket.getResolutionTimeMinutes() == null || ticket.getSlaResolutionTimeMinutes() == null) {
            return false;
        }
        return ticket.getResolutionTimeMinutes() <= ticket.getSlaResolutionTimeMinutes();
    }
}

