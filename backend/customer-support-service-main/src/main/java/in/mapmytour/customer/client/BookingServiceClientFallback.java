package in.mapmytour.customer.client;

import in.mapmytour.customer.dto.BookingDetailsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback implementation for BookingServiceClient
 * Provides default responses when booking service is unavailable
 */
@Component
@Slf4j
public class BookingServiceClientFallback implements BookingServiceClient {

    @Override
    public BookingDetailsDTO getBookingDetails(String bookingId) {
        log.warn("Booking service unavailable, returning fallback data for booking: {}", bookingId);
        return BookingDetailsDTO.builder()
                .bookingId(bookingId)
                .bookingReference("REF-" + (bookingId.length() > 8 ? bookingId.substring(0, 8) : bookingId))
                .bookingStatus("UNKNOWN")
                .paymentStatus("UNKNOWN")
                .build();
    }

    @Override
    public List<BookingDetailsDTO> getCustomerBookings(String customerId) {
        log.warn("Booking service unavailable, returning empty list for customer: {}", customerId);
        return new ArrayList<>();
    }
}

