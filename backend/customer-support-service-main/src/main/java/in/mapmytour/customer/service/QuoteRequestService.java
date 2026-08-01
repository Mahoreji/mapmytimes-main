package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.QuoteRequestDTO;
import in.mapmytour.customer.dto.QuoteResponseDTO;
import in.mapmytour.customer.dto.QuoteStatusUpdateDTO;
import in.mapmytour.customer.dto.QuoteSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuoteRequestService {
    QuoteResponseDTO createQuoteRequest(QuoteRequestDTO request);
    QuoteResponseDTO getQuoteRequestById(String id);
    Page<QuoteSummaryDTO> getAllQuoteRequests(Pageable pageable, String status);
    QuoteResponseDTO updateQuoteStatus(String id, QuoteStatusUpdateDTO updateDTO);
    Page<QuoteSummaryDTO> getQuoteRequestsByCustomer(String email, Pageable pageable);
    Page<QuoteSummaryDTO> getQuoteRequestsByStatus(String status, Pageable pageable);
    Page<QuoteSummaryDTO> searchQuoteRequests(String query, Pageable pageable);
    QuoteResponseDTO assignAgentToQuote(String quoteId, String agentId);
}