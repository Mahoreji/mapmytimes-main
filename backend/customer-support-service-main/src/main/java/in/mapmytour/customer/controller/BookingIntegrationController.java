package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.APIResponse;
import in.mapmytour.customer.dto.BookingDetailsDTO;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.service.BookingIntegrationService;
import in.mapmytour.customer.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingIntegrationController {

    private final BookingIntegrationService bookingIntegrationService;
    private final UserContextService userContextService;

    @GetMapping("/{bookingId}")
    public ResponseEntity<APIResponse<BookingDetailsDTO>> getBookingDetails(
            @PathVariable String bookingId) {
        
        log.info("Fetching booking details for: {}", bookingId);
        
        BookingDetailsDTO booking = bookingIntegrationService.getBookingDetails(bookingId);
        
        // Validate access - user can only access their own bookings unless admin
        if (!userContextService.isCurrentUserAdmin()) {
            String currentUserId = userContextService.getCurrentUserId();
            if (booking.getCustomerId() != null && !booking.getCustomerId().equals(currentUserId)) {
                throw new AccessDeniedException("Access denied: You can only access your own bookings");
            }
        }
        
        return ResponseEntity.ok(APIResponse.<BookingDetailsDTO>builder()
                .success(true)
                .statusCode(200)
                .message("Booking details retrieved successfully")
                .data(booking)
                .build());
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<APIResponse<List<BookingDetailsDTO>>> getMyBookings() {
        
        String currentUserId = userContextService.getCurrentUserId();
        log.info("Fetching bookings for user: {}", currentUserId);
        
        List<BookingDetailsDTO> bookings = bookingIntegrationService.getCustomerBookings(currentUserId);
        
        return ResponseEntity.ok(APIResponse.<List<BookingDetailsDTO>>builder()
                .success(true)
                .statusCode(200)
                .message("Your bookings retrieved successfully")
                .data(bookings)
                .build());
    }

    @PostMapping("/{bookingId}/link-ticket/{ticketId}")
    public ResponseEntity<APIResponse<Void>> linkTicketToBooking(
            @PathVariable String bookingId,
            @PathVariable String ticketId,
            @RequestParam(required = false) String bookingReference) {
        
        log.info("Linking ticket {} to booking {}", ticketId, bookingId);
        
        // Only admin users can manually link tickets
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException("Only administrators can link tickets to bookings");
        }
        
        bookingIntegrationService.linkTicketToBooking(ticketId, bookingId, bookingReference);
        
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Ticket linked to booking successfully")
                .build());
    }
}

