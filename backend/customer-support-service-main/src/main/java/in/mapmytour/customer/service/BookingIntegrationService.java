package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.BookingDetailsDTO;

/**
 * Service for integrating with booking system
 */
public interface BookingIntegrationService {
    
    /**
     * Get booking details by booking ID
     */
    BookingDetailsDTO getBookingDetails(String bookingId);
    
    /**
     * Link ticket to booking
     */
    void linkTicketToBooking(String ticketId, String bookingId, String bookingReference);
    
    /**
     * Get customer's booking history
     */
    java.util.List<BookingDetailsDTO> getCustomerBookings(String customerId);
    
    /**
     * Check if booking exists
     */
    boolean bookingExists(String bookingId);
}

