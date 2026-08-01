package in.mapmytour.customer.service;

import in.mapmytour.customer.entity.SupportTicket;

/**
 * Service for managing SLA (Service Level Agreement) tracking
 * Tracks response time and resolution time for tickets
 */
public interface SLAManagementService {
    
    /**
     * Calculate and update SLA metrics for a ticket
     */
    void updateSLAMetrics(SupportTicket ticket);
    
    /**
     * Record first response time when agent responds
     */
    void recordFirstResponse(String ticketId);
    
    /**
     * Calculate response time in minutes
     */
    Long calculateResponseTime(SupportTicket ticket);
    
    /**
     * Calculate resolution time in minutes
     */
    Long calculateResolutionTime(SupportTicket ticket);
    
    /**
     * Get SLA target based on priority
     */
    Integer getSlaResponseTimeMinutes(SupportTicket.TicketPriority priority);
    
    /**
     * Get SLA resolution target based on priority
     */
    Integer getSlaResolutionTimeMinutes(SupportTicket.TicketPriority priority);
    
    /**
     * Check if response SLA is met
     */
    boolean isResponseSLAMet(SupportTicket ticket);
    
    /**
     * Check if resolution SLA is met
     */
    boolean isResolutionSLAMet(SupportTicket ticket);
}

