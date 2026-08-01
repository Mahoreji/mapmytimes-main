package in.mapmytour.customer.service;

import in.mapmytour.customer.entity.SupportTicket;

/**
 * Service for automatic ticket escalation based on rules
 */
public interface EscalationService {
    
    /**
     * Check and escalate ticket if escalation rules are met
     */
    void checkAndEscalate(SupportTicket ticket);
    
    /**
     * Escalate ticket to next level
     */
    void escalateTicket(SupportTicket ticket, String reason);
    
    /**
     * Check if ticket should be escalated based on time
     */
    boolean shouldEscalateByTime(SupportTicket ticket);
    
    /**
     * Check if ticket should be escalated based on priority
     */
    boolean shouldEscalateByPriority(SupportTicket ticket);
    
    /**
     * Get escalation reason based on ticket state
     */
    String getEscalationReason(SupportTicket ticket);
}

