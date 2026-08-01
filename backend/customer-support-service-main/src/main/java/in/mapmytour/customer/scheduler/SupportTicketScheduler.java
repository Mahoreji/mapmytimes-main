package in.mapmytour.customer.scheduler;

import in.mapmytour.customer.entity.SupportTicket;
import in.mapmytour.customer.repository.SupportTicketRepository;
import in.mapmytour.customer.service.EscalationService;
import in.mapmytour.customer.service.SLAManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupportTicketScheduler {

    private final SupportTicketRepository ticketRepository;
    private final EscalationService escalationService;
    private final SLAManagementService slaManagementService;

    // Run every 60 seconds to process unresolved ticket SLAs and escalations
    @Scheduled(fixedRate = 60000)
    public void checkTicketsSlaAndEscalation() {
        log.debug("Starting scheduled check for unresolved ticket SLA and escalations");
        try {
            List<SupportTicket> unresolvedTickets = ticketRepository.findUnresolvedTickets();
            if (unresolvedTickets.isEmpty()) {
                return;
            }
            
            for (SupportTicket ticket : unresolvedTickets) {
                try {
                    // Update SLA metrics
                    slaManagementService.updateSLAMetrics(ticket);
                    
                    // Check and escalate if applicable
                    escalationService.checkAndEscalate(ticket);
                    
                    // Save ticket back to DB
                    ticketRepository.save(ticket);
                } catch (Exception e) {
                    log.error("Error processing SLA/escalation checks for ticket: {}", ticket.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching unresolved tickets for scheduler", e);
        }
    }
}
