package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.entity.QuoteRequest;
import in.mapmytour.customer.exception.ResourceNotFoundException;
import in.mapmytour.customer.exception.ServiceException;
import in.mapmytour.customer.repository.QuoteRequestRepository;
import in.mapmytour.customer.service.QuoteRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteRequestServiceImpl implements QuoteRequestService {

    private final QuoteRequestRepository quoteRequestRepository;

    @Override
    @Transactional
    public QuoteResponseDTO createQuoteRequest(QuoteRequestDTO request) {
        try {
            log.debug("Creating quote request for customer: {}", request.getPersonalInfo().getEmail());

            QuoteRequest quoteRequest = mapToEntity(request);
            quoteRequest.setId(UUID.randomUUID().toString());
            quoteRequest.setCreatedAt(LocalDate.now());
            quoteRequest.setUpdatedAt(LocalDate.now());
            quoteRequest.setStatus("PENDING");

            QuoteRequest savedRequest = quoteRequestRepository.save(quoteRequest);
            log.info("Quote request created successfully with ID: {}", savedRequest.getId());

            return mapToDTO(savedRequest);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating quote request", e);
            throw new ServiceException("Failed to create quote request due to data integrity violation");
        } catch (Exception e) {
            log.error("Unexpected error while creating quote request", e);
            throw new ServiceException("Failed to create quote request: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteResponseDTO getQuoteRequestById(String id) {
        try {
            log.debug("Fetching quote request with ID: {}", id);

            QuoteRequest quoteRequest = quoteRequestRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Quote request not found with id: " + id));

            return mapToDTO(quoteRequest);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching quote request with ID: {}", id, e);
            throw new ServiceException("Failed to fetch quote request: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuoteSummaryDTO> getAllQuoteRequests(Pageable pageable, String status) {
        try {
            log.debug("Fetching all quote requests with status: {}", status);

            if (StringUtils.hasText(status)) {
                return quoteRequestRepository.findByStatus(status, pageable)
                        .map(this::mapToSummaryDTO);
            } else {
                return quoteRequestRepository.findAll(pageable)
                        .map(this::mapToSummaryDTO);
            }
        } catch (Exception e) {
            log.error("Error fetching all quote requests", e);
            throw new ServiceException("Failed to fetch quote requests: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public QuoteResponseDTO updateQuoteStatus(String id, QuoteStatusUpdateDTO updateDTO) {
        try {
            log.debug("Updating quote request status for ID: {}", id);

            QuoteRequest quoteRequest = quoteRequestRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Quote request not found with id: " + id));

            quoteRequest.setStatus(updateDTO.getStatus());
            quoteRequest.setUpdatedAt(LocalDate.now());

            if (updateDTO.getAssignedAgentId() != null) {
                quoteRequest.setAssignedAgentId(updateDTO.getAssignedAgentId());
            }

            QuoteRequest updatedRequest = quoteRequestRepository.save(quoteRequest);
            log.info("Quote request status updated successfully for ID: {}", id);

            return mapToDTO(updatedRequest);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating quote request status for ID: {}", id, e);
            throw new ServiceException("Failed to update quote request status: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuoteSummaryDTO> getQuoteRequestsByCustomer(String email, Pageable pageable) {
        try {
            log.debug("Fetching quote requests for customer: {}", email);

            return quoteRequestRepository.findByPersonalInfoEmailIgnoreCase(email, pageable)
                    .map(this::mapToSummaryDTO);
        } catch (Exception e) {
            log.error("Error fetching quote requests for customer: {}", email, e);
            throw new ServiceException("Failed to fetch quote requests for customer: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuoteSummaryDTO> getQuoteRequestsByStatus(String status, Pageable pageable) {
        try {
            log.debug("Fetching quote requests with status: {}", status);

            return quoteRequestRepository.findByStatus(status, pageable)
                    .map(this::mapToSummaryDTO);
        } catch (Exception e) {
            log.error("Error fetching quote requests with status: {}", status, e);
            throw new ServiceException("Failed to fetch quote requests by status: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuoteSummaryDTO> searchQuoteRequests(String query, Pageable pageable) {
        try {
            log.debug("Searching quote requests with query: {}", query);

            if (!StringUtils.hasText(query)) {
                return getAllQuoteRequests(pageable, null);
            }

            return quoteRequestRepository.searchQuotes(query.trim(), pageable)
                    .map(this::mapToSummaryDTO);
        } catch (Exception e) {
            log.error("Error searching quote requests with query: {}", query, e);
            throw new ServiceException("Failed to search quote requests: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public QuoteResponseDTO assignAgentToQuote(String quoteId, String agentId) {
        try {
            log.debug("Assigning agent {} to quote {}", agentId, quoteId);

            QuoteRequest quoteRequest = quoteRequestRepository.findById(quoteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Quote request not found with id: " + quoteId));

            quoteRequest.setAssignedAgentId(agentId);
            quoteRequest.setStatus("PROCESSING");
            quoteRequest.setUpdatedAt(LocalDate.now());

            QuoteRequest updatedRequest = quoteRequestRepository.save(quoteRequest);
            log.info("Agent assigned successfully to quote: {}", quoteId);

            return mapToDTO(updatedRequest);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error assigning agent to quote: {}", quoteId, e);
            throw new ServiceException("Failed to assign agent to quote: " + e.getMessage());
        }
    }

    // Helper methods for mapping between entities and DTOs
    private QuoteRequest mapToEntity(QuoteRequestDTO dto) {
        return QuoteRequest.builder()
                .personalInfo(mapPersonalInfoToEntity(dto.getPersonalInfo()))
                .tripDetails(mapTripDetailsToEntity(dto.getTripDetails()))
                .accommodationPreferences(mapAccommodationToEntity(dto.getAccommodationPreferences()))
                .activitiesAndInclusions(mapActivitiesToEntity(dto.getActivitiesAndInclusions()))
                .budget(mapBudgetToEntity(dto.getBudget()))
                .consent(mapConsentToEntity(dto.getConsent()))
                .build();
    }

    private QuoteResponseDTO mapToDTO(QuoteRequest entity) {
        return QuoteResponseDTO.builder()
                .id(entity.getId())
                .personalInfo(mapPersonalInfoToDTO(entity.getPersonalInfo()))
                .tripDetails(mapTripDetailsToDTO(entity.getTripDetails()))
                .accommodationPreferences(mapAccommodationToDTO(entity.getAccommodationPreferences()))
                .activitiesAndInclusions(mapActivitiesToDTO(entity.getActivitiesAndInclusions()))
                .budget(mapBudgetToDTO(entity.getBudget()))
                .consent(mapConsentToDTO(entity.getConsent()))
                .createdAt(entity.getCreatedAt())
                .status(entity.getStatus())
                .assignedAgentId(entity.getAssignedAgentId())
                .quoteResponseId(entity.getQuoteResponseId())
                .build();
    }

    private QuoteSummaryDTO mapToSummaryDTO(QuoteRequest entity) {
        return QuoteSummaryDTO.builder()
                .id(entity.getId())
                .fullName(entity.getPersonalInfo().getFullName())
                .email(entity.getPersonalInfo().getEmail())
                .destination(entity.getTripDetails().getDestination())
                .departureDate(entity.getTripDetails().getDepartureDate())
                .returnDate(entity.getTripDetails().getReturnDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // Additional mapping methods for each nested object...
    private QuoteRequest.PersonalInfo mapPersonalInfoToEntity(QuoteRequestDTO.PersonalInfo dto) {
        return QuoteRequest.PersonalInfo.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .preferredContactMethod(QuoteRequest.ContactMethod.valueOf(dto.getPreferredContactMethod().name()))
                .build();
    }

    private QuoteRequest.TripDetails mapTripDetailsToEntity(QuoteRequestDTO.TripDetails dto) {
        return QuoteRequest.TripDetails.builder()
                .destination(dto.getDestination())
                .departureCity(dto.getDepartureCity())
                .departureDate(dto.getDepartureDate())
                .returnDate(dto.getReturnDate())
                .flexibleDates(dto.isFlexibleDates())
                .numberOfAdults(dto.getNumberOfAdults())
                .numberOfChildren(dto.getNumberOfChildren())
                .travelType(QuoteRequest.TravelType.valueOf(dto.getTravelType().name()))
                .build();
    }

    private QuoteRequest.AccommodationPreferences mapAccommodationToEntity(
            QuoteRequestDTO.AccommodationPreferences dto) {
        return QuoteRequest.AccommodationPreferences.builder()
                .hotelCategory(QuoteRequest.HotelCategory.valueOf(dto.getHotelCategory().name()))
                .numberOfRooms(dto.getNumberOfRooms())
                .roomType(QuoteRequest.RoomType.valueOf(dto.getRoomType().name()))
                .build();
    }

    private QuoteRequest.ActivitiesAndInclusions mapActivitiesToEntity(
            QuoteRequestDTO.ActivitiesAndInclusions dto) {
        return QuoteRequest.ActivitiesAndInclusions.builder()
                .interestedActivities(dto.getInterestedActivities())
                .needGuide(dto.isNeedGuide())
                .includeFlights(dto.isIncludeFlights())
                .includeMeals(dto.isIncludeMeals())
                .specialRequests(dto.getSpecialRequests())
                .build();
    }

    private QuoteRequest.Budget mapBudgetToEntity(QuoteRequestDTO.Budget dto) {
        return QuoteRequest.Budget.builder()
                .estimatedBudget(dto.getEstimatedBudget())
                .isBudgetFlexible(dto.isBudgetFlexible())
                .build();
    }

    private QuoteRequest.Consent mapConsentToEntity(QuoteRequestDTO.Consent dto) {
        return QuoteRequest.Consent.builder()
                .acceptTerms(dto.isAcceptTerms())
                .subscribeNewsletter(dto.isSubscribeNewsletter())
                .build();
    }

    // Response mapping methods
    private QuoteResponseDTO.PersonalInfo mapPersonalInfoToDTO(QuoteRequest.PersonalInfo entity) {
        return QuoteResponseDTO.PersonalInfo.builder()
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .preferredContactMethod(QuoteRequestDTO.ContactMethod.valueOf(entity.getPreferredContactMethod().name()))
                .build();
    }

    private QuoteResponseDTO.TripDetails mapTripDetailsToDTO(QuoteRequest.TripDetails entity) {
        return QuoteResponseDTO.TripDetails.builder()
                .destination(entity.getDestination())
                .departureCity(entity.getDepartureCity())
                .departureDate(entity.getDepartureDate())
                .returnDate(entity.getReturnDate())
                .flexibleDates(entity.isFlexibleDates())
                .numberOfAdults(entity.getNumberOfAdults())
                .numberOfChildren(entity.getNumberOfChildren())
                .travelType(QuoteRequestDTO.TravelType.valueOf(entity.getTravelType().name()))
                .build();
    }

    private QuoteResponseDTO.AccommodationPreferences mapAccommodationToDTO(
            QuoteRequest.AccommodationPreferences entity) {
        return QuoteResponseDTO.AccommodationPreferences.builder()
                .hotelCategory(QuoteRequestDTO.HotelCategory.valueOf(entity.getHotelCategory().name()))
                .numberOfRooms(entity.getNumberOfRooms())
                .roomType(QuoteRequestDTO.RoomType.valueOf(entity.getRoomType().name()))
                .build();
    }

    private QuoteResponseDTO.ActivitiesAndInclusions mapActivitiesToDTO(
            QuoteRequest.ActivitiesAndInclusions entity) {
        return QuoteResponseDTO.ActivitiesAndInclusions.builder()
                .interestedActivities(entity.getInterestedActivities())
                .needGuide(entity.isNeedGuide())
                .includeFlights(entity.isIncludeFlights())
                .includeMeals(entity.isIncludeMeals())
                .specialRequests(entity.getSpecialRequests())
                .build();
    }

    private QuoteResponseDTO.Budget mapBudgetToDTO(QuoteRequest.Budget entity) {
        return QuoteResponseDTO.Budget.builder()
                .estimatedBudget(entity.getEstimatedBudget())
                .isBudgetFlexible(entity.isBudgetFlexible())
                .build();
    }

    private QuoteResponseDTO.Consent mapConsentToDTO(QuoteRequest.Consent entity) {
        return QuoteResponseDTO.Consent.builder()
                .acceptTerms(entity.isAcceptTerms())
                .subscribeNewsletter(entity.isSubscribeNewsletter())
                .build();
    }
}