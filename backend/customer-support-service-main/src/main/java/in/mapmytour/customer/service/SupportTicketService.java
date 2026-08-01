package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupportTicketService {
    TicketResponse createTicket(CreateTicketRequest request);
    TicketResponse getTicketById(String id);
    Page<TicketResponse> getAllTickets(Pageable pageable, String status, String category, String priority);
    TicketResponse updateTicket(String id, UpdateTicketRequest request);
    TicketResponse updateTicketStatus(String id, TicketStatusUpdateRequest request);
    Page<TicketSummaryResponse> getTicketsByCustomer(String customerId, Pageable pageable);
    TicketResponse assignTicket( String id, String agentId);
    Page<TicketResponse> searchTickets( String query, Pageable pageable);
}