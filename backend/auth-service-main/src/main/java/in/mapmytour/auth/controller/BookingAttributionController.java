package in.mapmytour.auth.controller;

import in.mapmytour.auth.dto.APIResponse;
import in.mapmytour.auth.dto.user.BookingAttributionRequest;
import in.mapmytour.auth.dto.user.BookingAttributionResponse;
import in.mapmytour.auth.service.TripCircleService;
import in.mapmytour.auth.utils.APIResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller used by booking-service to record booking attributions.
 * This endpoint is expected to be called by internal services only
 * (e.g., behind API Gateway with proper authentication).
 */
@RestController
@RequestMapping("/api/v1/attribution")
@RequiredArgsConstructor
@Slf4j
public class BookingAttributionController {

    private final TripCircleService tripCircleService;

    @PostMapping("/bookings")
    public ResponseEntity<APIResponse<BookingAttributionResponse>> recordAttribution(
            @Valid @RequestBody BookingAttributionRequest request) {
        try {
            BookingAttributionResponse response = tripCircleService.recordBookingAttribution(request);
            String message = response.isCreated() ? "Attribution recorded" : "Attribution already exists";
            return APIResponseUtil.success(response, message);
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to record booking attribution", ex);
            return APIResponseUtil.internalServerError("Failed to record booking attribution");
        }
    }
}
