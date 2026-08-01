package in.mapmytour.customer.client;

import in.mapmytour.customer.dto.BookingDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign Client for Booking Service Integration
 * Provides declarative HTTP client for booking service communication
 */
@FeignClient(
    name = "booking-service",
    url = "${booking.service.url:http://booking-service:8089}"
)
public interface BookingServiceClient {

    /**
     * Get booking details by booking ID
     * @param bookingId The booking ID
     * @return BookingDetailsDTO with booking information
     */
    @GetMapping("/api/v1/bookings/{bookingId}")
    BookingDetailsDTO getBookingDetails(@PathVariable("bookingId") String bookingId);

    /**
     * Get all bookings for a customer
     * @param customerId The customer ID
     * @return List of BookingDetailsDTO
     */
    @GetMapping("/api/v1/bookings/customer/{customerId}")
    List<BookingDetailsDTO> getCustomerBookings(@PathVariable("customerId") String customerId);
}

