package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerFeedbackService {
    FeedbackResponse submitFeedback(SubmitFeedbackRequest request);
    FeedbackResponse getFeedbackById(String id);
    Page<FeedbackResponse> getFeedbackByCustomer(String customerId, Pageable pageable);
    Page<FeedbackResponse> getFeedbackByTicket(String ticketId, Pageable pageable);
    Page<FeedbackResponse> getAllFeedback(Pageable pageable, Integer minRating, Integer maxRating);
    FeedbackStatsResponse getFeedbackStats();
    void deleteFeedback(String id);
}