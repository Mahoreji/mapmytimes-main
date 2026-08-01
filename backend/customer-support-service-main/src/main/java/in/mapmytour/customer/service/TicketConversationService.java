package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketConversationService {
    ConversationResponse addConversation(CreateConversationRequest request);
    Page<ConversationResponse> getConversationsByTicket(String ticketId, Pageable pageable, boolean includeInternal);
    ConversationResponse updateConversation( String conversationId, UpdateConversationRequest request);
}