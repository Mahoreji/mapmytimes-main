package in.mapmytour.customer.service;

/**
 * Service for sending notifications to customers and agents
 */
public interface NotificationService {
    
    /**
     * Send ticket created notification to customer
     * @param ticketId Ticket ID
     * @param customerId Customer ID
     * @param customerEmail Customer email
     * @param subject Ticket subject
     */
    void sendTicketCreatedNotification(String ticketId, String customerId, String customerEmail, String subject);
    
    /**
     * Send ticket updated notification
     * @param ticketId Ticket ID
     * @param customerId Customer ID
     * @param customerEmail Customer email
     * @param updateMessage Update message
     */
    void sendTicketUpdatedNotification(String ticketId, String customerId, String customerEmail, String updateMessage);
    
    /**
     * Send ticket resolved notification
     * @param ticketId Ticket ID
     * @param customerId Customer ID
     * @param customerEmail Customer email
     * @param resolutionMessage Resolution message
     */
    void sendTicketResolvedNotification(String ticketId, String customerId, String customerEmail, String resolutionMessage);
    
    /**
     * Send ticket escalation notification to agent
     * @param ticketId Ticket ID
     * @param agentId Agent ID
     * @param agentEmail Agent email
     * @param escalationReason Escalation reason
     */
    void sendEscalationNotification(String ticketId, String agentId, String agentEmail, String escalationReason);
    
    /**
     * Send agent assignment notification
     * @param ticketId Ticket ID
     * @param agentId Agent ID
     * @param agentEmail Agent email
     * @param ticketSubject Ticket subject
     */
    void sendAgentAssignmentNotification(String ticketId, String agentId, String agentEmail, String ticketSubject);
}

