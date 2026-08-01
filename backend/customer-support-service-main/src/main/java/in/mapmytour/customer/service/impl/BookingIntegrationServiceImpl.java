package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.client.BookingServiceClient;
import in.mapmytour.customer.dto.BookingDetailsDTO;
import in.mapmytour.customer.entity.SupportTicket;
import in.mapmytour.customer.repository.SupportTicketRepository;
import in.mapmytour.customer.service.BookingIntegrationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingIntegrationServiceImpl implements BookingIntegrationService {

    private final SupportTicketRepository ticketRepository;
    private final BookingServiceClient bookingServiceClient;

    @Override
    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookingDetailsFallback")
    @Retry(name = "bookingService")
    public BookingDetailsDTO getBookingDetails(String bookingId) {
        log.debug("Fetching booking details for booking ID: {}", bookingId);
        BookingDetailsDTO booking = bookingServiceClient.getBookingDetails(bookingId);
        if (booking == null || booking.getBookingId() == null) {
            throw new RuntimeException("Booking not found: " + bookingId);
        }
        return booking;
    }
    
    /**
     * Fallback method for getBookingDetails when circuit breaker is open or service fails
     */
    public BookingDetailsDTO getBookingDetailsFallback(String bookingId, Throwable e) {
        if (e != null) {
            log.warn("Using fallback for booking details: {} due to: {}", bookingId, e.getMessage());
        } else {
            log.warn("Using fallback for booking details: {}", bookingId);
        }
        return BookingDetailsDTO.builder()
                .bookingId(bookingId)
                .bookingReference("REF-" + (bookingId.length() > 8 ? bookingId.substring(0, 8) : bookingId))
                .bookingStatus("UNKNOWN")
                .paymentStatus("UNKNOWN")
                .build();
    }

    @Override
    public void linkTicketToBooking(String ticketId, String bookingId, String bookingReference) {
        log.debug("Linking ticket {} to booking {}", ticketId, bookingId);
        
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        
        ticket.setBookingId(bookingId);
        ticket.setBookingReference(bookingReference);
        ticketRepository.save(ticket);
    }

    @Override
    @CircuitBreaker(name = "bookingService", fallbackMethod = "getCustomerBookingsFallback")
    @Retry(name = "bookingService")
    public List<BookingDetailsDTO> getCustomerBookings(String customerId) {
        log.debug("Fetching bookings for customer: {}", customerId);
        List<BookingDetailsDTO> bookings = bookingServiceClient.getCustomerBookings(customerId);
        if (bookings == null) {
            throw new RuntimeException("No bookings returned for customer: " + customerId);
        }
        return bookings;
    }
    
    /**
     * Fallback method for getCustomerBookings when circuit breaker is open or service fails
     */
    public List<BookingDetailsDTO> getCustomerBookingsFallback(String customerId, Throwable e) {
        if (e != null) {
            log.warn("Using fallback for customer bookings: {} due to: {}", customerId, e.getMessage());
        } else {
            log.warn("Using fallback for customer bookings: {}", customerId);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean bookingExists(String bookingId) {
        try {
            BookingDetailsDTO booking = getBookingDetails(bookingId);
            return booking != null && booking.getBookingId() != null;
        } catch (Exception e) {
            return false;
        }
    }
}

