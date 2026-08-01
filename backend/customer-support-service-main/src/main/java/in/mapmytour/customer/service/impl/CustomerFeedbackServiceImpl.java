package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.entity.CustomerFeedback;
import in.mapmytour.customer.exception.ResourceNotFoundException;
import in.mapmytour.customer.exception.ServiceException;
import in.mapmytour.customer.repository.CustomerFeedbackRepository;
import in.mapmytour.customer.service.CustomerFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerFeedbackServiceImpl implements CustomerFeedbackService {

    private final CustomerFeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public FeedbackResponse submitFeedback(SubmitFeedbackRequest request) {
        try {
            log.debug("Submitting feedback for customer: {}", request.getCustomerId());

            // Validate rating is within range
            if (request.getRating() < 1 || request.getRating() > 5) {
                throw new ServiceException("Rating must be between 1 and 5");
            }

            // Check if feedback already exists for this ticket
            if (request.getTicketId() != null && feedbackRepository.existsByTicketId(request.getTicketId())) {
                throw new ServiceException("Feedback already exists for this ticket");
            }

            CustomerFeedback feedback = CustomerFeedback.builder()
                    .id(UUID.randomUUID().toString())
                    .customerId(request.getCustomerId())
                    .ticketId(request.getTicketId())
                    .rating(request.getRating())
                    .comments(request.getComments())
                    .isFollowUpRequired(request.isFollowUpRequired())
                    .submittedAt(LocalDateTime.now())
                    .build();

            CustomerFeedback savedFeedback = feedbackRepository.save(feedback);
            log.info("Feedback submitted successfully with ID: {}", savedFeedback.getId());

            return mapToFeedbackResponse(savedFeedback);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while submitting feedback", e);
            throw new ServiceException("Failed to submit feedback due to data integrity violation");
        } catch (Exception e) {
            log.error("Unexpected error while submitting feedback", e);
            throw new ServiceException("Failed to submit feedback: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(String id) {
        try {
            log.debug("Fetching feedback with ID: {}", id);

            CustomerFeedback feedback = feedbackRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with id: " + id));

            return mapToFeedbackResponse(feedback);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching feedback with ID: {}", id, e);
            throw new ServiceException("Failed to fetch feedback: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getFeedbackByCustomer(String customerId, Pageable pageable) {
        try {
            log.debug("Fetching feedback for customer: {}", customerId);

            return feedbackRepository.findByCustomerId(customerId, pageable)
                    .map(this::mapToFeedbackResponse);
        } catch (Exception e) {
            log.error("Error fetching feedback for customer: {}", customerId, e);
            throw new ServiceException("Failed to fetch feedback for customer: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getFeedbackByTicket(String ticketId, Pageable pageable) {
        try {
            log.debug("Fetching feedback for ticket: {}", ticketId);

            return feedbackRepository.findByTicketId(ticketId, pageable)
                    .map(this::mapToFeedbackResponse);
        } catch (Exception e) {
            log.error("Error fetching feedback for ticket: {}", ticketId, e);
            throw new ServiceException("Failed to fetch feedback for ticket: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedback(Pageable pageable, Integer minRating, Integer maxRating) {
        try {
            log.debug("Fetching all feedback with rating range: {} - {}", minRating, maxRating);

            if (minRating != null && maxRating != null) {
                return feedbackRepository.findByRatingRange(minRating, maxRating, pageable)
                        .map(this::mapToFeedbackResponse);
            } else {
                return feedbackRepository.findAll(pageable)
                        .map(this::mapToFeedbackResponse);
            }
        } catch (Exception e) {
            log.error("Error fetching all feedback", e);
            throw new ServiceException("Failed to fetch feedback: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackStatsResponse getFeedbackStats() {
        try {
            log.debug("Calculating feedback statistics");

            Double averageRating = feedbackRepository.findAverageRating();
            if (averageRating == null) {
                averageRating = 0.0;
            }

            long totalFeedbacks = feedbackRepository.count();
            long positiveFeedbacks = feedbackRepository.countByRatingGreaterThanEqual(4);
            long neutralFeedbacks = feedbackRepository.countByRating(3);
            long negativeFeedbacks = feedbackRepository.countByRatingGreaterThanEqual(1) -
                    feedbackRepository.countByRatingGreaterThanEqual(3);

            return FeedbackStatsResponse.builder()
                    .averageRating(averageRating)
                    .totalFeedbacks((int) totalFeedbacks)
                    .positiveFeedbacks((int) positiveFeedbacks)
                    .neutralFeedbacks((int) neutralFeedbacks)
                    .negativeFeedbacks((int) negativeFeedbacks)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating feedback statistics", e);
            throw new ServiceException("Failed to calculate feedback statistics: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteFeedback(String id) {
        try {
            log.debug("Deleting feedback with ID: {}", id);

            if (!feedbackRepository.existsById(id)) {
                throw new ResourceNotFoundException("Feedback not found with id: " + id);
            }

            feedbackRepository.deleteById(id);
            log.info("Feedback deleted successfully with ID: {}", id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting feedback with ID: {}", id, e);
            throw new ServiceException("Failed to delete feedback: " + e.getMessage());
        }
    }

    private FeedbackResponse mapToFeedbackResponse(CustomerFeedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .ticketId(feedback.getTicketId())
                .customerId(feedback.getCustomerId())
                .rating(feedback.getRating())
                .comments(feedback.getComments())
                .isFollowUpRequired(feedback.isFollowUpRequired())
                .submittedAt(feedback.getSubmittedAt())
                .build();
    }
}