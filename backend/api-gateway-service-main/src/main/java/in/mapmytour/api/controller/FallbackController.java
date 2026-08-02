package in.mapmytour.api.controller;

import in.mapmytour.api.dto.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    private String getFallbackTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // ==================== AUTH & USER SERVICE FALLBACKS ====================

    @GetMapping("/auth")
    public ResponseEntity<APIResponse<Object>> authServiceFallback() {
        log.warn("Auth service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Authentication service is temporarily unavailable. Please try again in a few moments.",
                "AUTH_SERVICE_DOWN"
        );
    }

    @PostMapping("/auth")
    public ResponseEntity<APIResponse<Object>> authServiceFallbackPost() {
        return authServiceFallback();
    }

    @GetMapping("/user")
    public ResponseEntity<APIResponse<Object>> userServiceFallback() {
        log.warn("User service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "User management service is temporarily unavailable. Your account data is safe.",
                "USER_SERVICE_DOWN"
        );
    }

    @PostMapping("/user")
    public ResponseEntity<APIResponse<Object>> userServiceFallbackPost() {
        return userServiceFallback();
    }

    // ==================== PAYMENT SERVICE FALLBACKS ====================

    @GetMapping("/payment")
    public ResponseEntity<APIResponse<Object>> paymentServiceFallback() {
        log.warn("Payment service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Payment processing is temporarily unavailable. No charges have been made. Please try again later.",
                "PAYMENT_SERVICE_DOWN"
        );
    }

    @PostMapping("/payment")
    public ResponseEntity<APIResponse<Object>> paymentServiceFallbackPost() {
        return paymentServiceFallback();
    }

    // ==================== BOOKING SERVICE FALLBACKS ====================

    @GetMapping("/bookings")
    public ResponseEntity<APIResponse<Object>> bookingServiceFallback() {
        log.warn("Booking service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Booking service is temporarily unavailable. Your existing bookings are safe.",
                "BOOKING_SERVICE_DOWN"
        );
    }

    @PostMapping("/bookings")
    public ResponseEntity<APIResponse<Object>> bookingServiceFallbackPost() {
        return bookingServiceFallback();
    }

    // ==================== TRAVEL SERVICE FALLBACKS ====================

    @GetMapping("/travel")
    public ResponseEntity<APIResponse<Object>> travelServiceFallback() {
        log.warn("Travel service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Travel information service is temporarily unavailable. Please check back soon for travel updates.",
                "TRAVEL_SERVICE_DOWN"
        );
    }

    @PostMapping("/travel")
    public ResponseEntity<APIResponse<Object>> travelServiceFallbackPost() {
        return travelServiceFallback();
    }

    // ==================== REVIEWS SERVICE FALLBACKS ====================

    @GetMapping("/reviews")
    public ResponseEntity<APIResponse<Object>> reviewsServiceFallback() {
        log.warn("Reviews service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Reviews service is temporarily unavailable. You can still browse other content.",
                "REVIEWS_SERVICE_DOWN"
        );
    }

    @PostMapping("/reviews")
    public ResponseEntity<APIResponse<Object>> reviewsServiceFallbackPost() {
        return reviewsServiceFallback();
    }

    // ==================== BLOG SERVICE FALLBACKS ====================

    @GetMapping("/blog")
    public ResponseEntity<APIResponse<Object>> blogServiceFallback() {
        log.warn("Blog service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Blog service is temporarily unavailable. Check our social media for latest updates.",
                "BLOG_SERVICE_DOWN"
        );
    }

    @PostMapping("/blog")
    public ResponseEntity<APIResponse<Object>> blogServiceFallbackPost() {
        return blogServiceFallback();
    }

    // ==================== CUSTOMER SUPPORT SERVICE FALLBACKS ====================

    @GetMapping("/customer-support")
    public ResponseEntity<APIResponse<Object>> customerSupportServiceFallback() {
        log.warn("Customer support service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Customer support system is temporarily unavailable. For urgent matters, please email support@mapmytimes.com",
                "CUSTOMER_SUPPORT_SERVICE_DOWN"
        );
    }

    @PostMapping("/customer-support")
    public ResponseEntity<APIResponse<Object>> customerSupportServiceFallbackPost() {
        return customerSupportServiceFallback();
    }

    // ==================== UTILS SERVICE FALLBACKS ====================

    @GetMapping("/utils")
    public ResponseEntity<APIResponse<Object>> utilsServiceFallback() {
        log.warn("Utils service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Utility services are temporarily unavailable. Core features are still accessible.",
                "UTILS_SERVICE_DOWN"
        );
    }

    @PostMapping("/utils")
    public ResponseEntity<APIResponse<Object>> utilsServiceFallbackPost() {
        return utilsServiceFallback();
    }

    // ==================== CORE SERVICE FALLBACKS ====================

    @GetMapping("/core")
    public ResponseEntity<APIResponse<Object>> coreServiceFallback() {
        log.warn("Core service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Core travel services (tours, destinations, activities) are temporarily unavailable. Please try again shortly.",
                "CORE_SERVICE_DOWN"
        );
    }

    @PostMapping("/core")
    public ResponseEntity<APIResponse<Object>> coreServiceFallbackPost() {
        return coreServiceFallback();
    }

    @PatchMapping("/core")
    public ResponseEntity<APIResponse<Object>> coreServiceFallbackPatch() {
        return coreServiceFallback();
    }

    @DeleteMapping("/core")
    public ResponseEntity<APIResponse<Object>> coreServiceFallbackDelete() {
        return coreServiceFallback();
    }

    // ==================== CHAT SERVICE FALLBACKS ====================

    @GetMapping("/chat")
    public ResponseEntity<APIResponse<Object>> chatServiceFallback() {
        log.warn("Chat service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "AI Chat assistant is temporarily unavailable. You can still browse and book tours manually.",
                "CHAT_SERVICE_DOWN"
        );
    }

    @PostMapping("/chat")
    public ResponseEntity<APIResponse<Object>> chatServiceFallbackPost() {
        return chatServiceFallback();
    }

    // ==================== NOTIFICATION SERVICE FALLBACKS ====================

    @GetMapping("/notification")
    public ResponseEntity<APIResponse<Object>> notificationServiceFallback() {
        log.warn("Notification service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Notification service is temporarily unavailable. Your notifications will be queued and sent once the service is restored.",
                "NOTIFICATION_SERVICE_DOWN"
        );
    }

    @PostMapping("/notification")
    public ResponseEntity<APIResponse<Object>> notificationServiceFallbackPost() {
        return notificationServiceFallback();
    }

    // ==================== HOTEL SERVICE FALLBACKS ====================

    @GetMapping("/hotel")
    public ResponseEntity<APIResponse<Object>> hotelServiceFallback() {
        log.warn("Hotel service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Hotel service is temporarily unavailable. Please try again in a few moments.",
                "HOTEL_SERVICE_DOWN"
        );
    }

    @PostMapping("/hotel")
    public ResponseEntity<APIResponse<Object>> hotelServiceFallbackPost() {
        return hotelServiceFallback();
    }

    // ==================== EMPLOYEE SERVICE FALLBACKS ====================

    @GetMapping("/employee")
    public ResponseEntity<APIResponse<Object>> employeeServiceFallback() {
        log.warn("Employee service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Employee management service is temporarily unavailable. Please try again later.",
                "EMPLOYEE_SERVICE_DOWN"
        );
    }

    @PostMapping("/employee")
    public ResponseEntity<APIResponse<Object>> employeeServiceFallbackPost() {
        return employeeServiceFallback();
    }

    // ==================== AGENT SERVICE FALLBACKS ====================

    @GetMapping("/agent")
    public ResponseEntity<APIResponse<Object>> agentServiceFallback() {
        log.warn("Agent service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Agent portal service is temporarily unavailable. Please try again in a few moments.",
                "AGENT_SERVICE_DOWN"
        );
    }

    @PostMapping("/agent")
    public ResponseEntity<APIResponse<Object>> agentServiceFallbackPost() {
        return agentServiceFallback();
    }

    // ==================== LEAD SERVICE FALLBACKS ====================

    @GetMapping("/lead")
    public ResponseEntity<APIResponse<Object>> leadServiceFallback() {
        log.warn("Lead service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Lead management service is temporarily unavailable. Your leads are safe and will be processed once service is restored.",
                "LEAD_SERVICE_DOWN"
        );
    }

    @PostMapping("/lead")
    public ResponseEntity<APIResponse<Object>> leadServiceFallbackPost() {
        return leadServiceFallback();
    }

    // ==================== SUPPLIER SERVICE FALLBACKS ====================

    @GetMapping("/supplier")
    public ResponseEntity<APIResponse<Object>> supplierServiceFallback() {
        log.warn("Supplier service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Supplier portal service is temporarily unavailable. Please try again later.",
                "SUPPLIER_SERVICE_DOWN"
        );
    }

    @PostMapping("/supplier")
    public ResponseEntity<APIResponse<Object>> supplierServiceFallbackPost() {
        return supplierServiceFallback();
    }

    // ==================== GST SERVICE FALLBACKS ====================

    @GetMapping("/gst")
    public ResponseEntity<APIResponse<Object>> gstServiceFallback() {
        log.warn("GST service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "GST and accounting service is temporarily unavailable. Financial data is safe.",
                "GST_SERVICE_DOWN"
        );
    }

    @PostMapping("/gst")
    public ResponseEntity<APIResponse<Object>> gstServiceFallbackPost() {
        return gstServiceFallback();
    }

    // ==================== FRAUD SERVICE FALLBACKS ====================

    @GetMapping("/fraud")
    public ResponseEntity<APIResponse<Object>> fraudServiceFallback() {
        log.warn("Fraud detection service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Fraud detection service is temporarily unavailable. Security measures are still active.",
                "FRAUD_SERVICE_DOWN"
        );
    }

    @PostMapping("/fraud")
    public ResponseEntity<APIResponse<Object>> fraudServiceFallbackPost() {
        return fraudServiceFallback();
    }

    // ==================== DOCUMENT SERVICE FALLBACKS ====================

    @GetMapping("/document")
    public ResponseEntity<APIResponse<Object>> documentServiceFallback() {
        log.warn("Document service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Document management service is temporarily unavailable. Your documents are safely stored.",
                "DOCUMENT_SERVICE_DOWN"
        );
    }

    @PostMapping("/document")
    public ResponseEntity<APIResponse<Object>> documentServiceFallbackPost() {
        return documentServiceFallback();
    }

    // ==================== AUDIT SERVICE FALLBACKS ====================

    @GetMapping("/audit")
    public ResponseEntity<APIResponse<Object>> auditServiceFallback() {
        log.warn("Audit service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Audit log service is temporarily unavailable. All activities are being logged and will be processed once service is restored.",
                "AUDIT_SERVICE_DOWN"
        );
    }

    @PostMapping("/audit")
    public ResponseEntity<APIResponse<Object>> auditServiceFallbackPost() {
        return auditServiceFallback();
    }

    // ==================== REPORT SERVICE FALLBACKS ====================

    @GetMapping("/report")
    public ResponseEntity<APIResponse<Object>> reportServiceFallback() {
        log.warn("Report service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Report and analytics service is temporarily unavailable. Please try again later.",
                "REPORT_SERVICE_DOWN"
        );
    }

    @PostMapping("/report")
    public ResponseEntity<APIResponse<Object>> reportServiceFallbackPost() {
        return reportServiceFallback();
    }

    // ==================== GROUP BOOKING SERVICE FALLBACKS ====================

    @GetMapping("/group-booking")
    public ResponseEntity<APIResponse<Object>> groupBookingServiceFallback() {
        log.warn("Group booking service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Group booking service is temporarily unavailable. Please try again in a few moments.",
                "GROUP_BOOKING_SERVICE_DOWN"
        );
    }

    @PostMapping("/group-booking")
    public ResponseEntity<APIResponse<Object>> groupBookingServiceFallbackPost() {
        return groupBookingServiceFallback();
    }

    // ==================== CORPORATE TRAVEL SERVICE FALLBACKS ====================

    @GetMapping("/corporate-travel")
    public ResponseEntity<APIResponse<Object>> corporateTravelServiceFallback() {
        log.warn("Corporate travel service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Corporate travel management service is temporarily unavailable. Please try again later.",
                "CORPORATE_TRAVEL_SERVICE_DOWN"
        );
    }

    @PostMapping("/corporate-travel")
    public ResponseEntity<APIResponse<Object>> corporateTravelServiceFallbackPost() {
        return corporateTravelServiceFallback();
    }

    // ==================== LOYALTY SERVICE FALLBACKS ====================

    @GetMapping("/loyalty")
    public ResponseEntity<APIResponse<Object>> loyaltyServiceFallback() {
        log.warn("Loyalty service is currently unavailable - fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "Loyalty and rewards service is temporarily unavailable. Your points and rewards are safe.",
                "LOYALTY_SERVICE_DOWN"
        );
    }

    @PostMapping("/loyalty")
    public ResponseEntity<APIResponse<Object>> loyaltyServiceFallbackPost() {
        return loyaltyServiceFallback();
    }

    // ==================== COMMON FALLBACKS ====================

    @GetMapping("/common")
    public ResponseEntity<APIResponse<Object>> commonFallback() {
        log.warn("Common service fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "The requested service is temporarily unavailable. Our team has been notified.",
                "COMMON_SERVICE_DOWN"
        );
    }

    @PostMapping("/common")
    public ResponseEntity<APIResponse<Object>> commonFallbackPost() {
        return commonFallback();
    }

    @GetMapping
    public ResponseEntity<APIResponse<Object>> genericFallback() {
        log.warn("Generic service fallback triggered at {}", getFallbackTimestamp());
        return createServiceUnavailableResponse(
                "The requested service is temporarily unavailable. Please try again in a few minutes.",
                "GENERIC_SERVICE_DOWN"
        );
    }

    // ==================== HEALTH & STATUS ENDPOINTS ====================

    @GetMapping("/health")
    public ResponseEntity<APIResponse<Object>> healthCheck() {
        APIResponse<Object> response = APIResponse.builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("API Gateway fallback endpoints are healthy")
                .data(createHealthData())
                .errors(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<APIResponse<Object>> statusCheck() {
        APIResponse<Object> response = APIResponse.builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Gateway fallback system operational")
                .data(createStatusData())
                .errors(null)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==================== HELPER METHODS ====================

    private ResponseEntity<APIResponse<Object>> createServiceUnavailableResponse(String message, String errorCode) {
        APIResponse<Object> response = APIResponse.builder()
                .success(false)
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message(message)
                .data(createFallbackData(errorCode))
                .errors(null)
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    private Object createFallbackData(String errorCode) {
        return new FallbackData(
                errorCode,
                "Service temporarily unavailable",
                getFallbackTimestamp(),
                "Please try again in a few minutes or contact support if the issue persists."
        );
    }

    private Object createHealthData() {
        return new HealthData(
                "Gateway Fallback System",
                "healthy",
                getFallbackTimestamp(),
                "All fallback endpoints are operational"
        );
    }

    private Object createStatusData() {
        return new StatusData(
                "API Gateway Fallback Controller",
                "1.0.0",
                getFallbackTimestamp(),
                "Handling service unavailability gracefully"
        );
    }

    // ==================== DATA CLASSES ====================

    public static class FallbackData {
        public final String errorCode;
        public final String status;
        public final String timestamp;
        public final String recommendation;

        public FallbackData(String errorCode, String status, String timestamp, String recommendation) {
            this.errorCode = errorCode;
            this.status = status;
            this.timestamp = timestamp;
            this.recommendation = recommendation;
        }
    }

    public static class HealthData {
        public final String service;
        public final String status;
        public final String timestamp;
        public final String message;

        public HealthData(String service, String status, String timestamp, String message) {
            this.service = service;
            this.status = status;
            this.timestamp = timestamp;
            this.message = message;
        }
    }

    public static class StatusData {
        public final String component;
        public final String version;
        public final String timestamp;
        public final String description;

        public StatusData(String component, String version, String timestamp, String description) {
            this.component = component;
            this.version = version;
            this.timestamp = timestamp;
            this.description = description;
        }
    }
}