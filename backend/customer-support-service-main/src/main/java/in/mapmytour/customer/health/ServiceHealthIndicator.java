package in.mapmytour.customer.health;

import in.mapmytour.customer.client.BookingServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Health Indicator for external service dependencies
 * Checks connectivity to booking service and other dependencies
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceHealthIndicator implements HealthIndicator {

    private final BookingServiceClient bookingServiceClient;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        try {
            // Check booking service connectivity (with timeout and graceful failure)
            try {
                bookingServiceClient.getBookingDetails("health-check");
                builder.withDetail("booking-service", "UP");
            } catch (Exception e) {
                // Don't mark overall health as DOWN if external services are unavailable
                // Circuit breakers handle this gracefully
                builder.withDetail("booking-service", "DOWN")
                       .withDetail("booking-service-error", "Service unavailable (circuit breaker may be open)")
                       .withDetail("booking-service-note", "Service will use fallback responses");
                // Don't call builder.down() - allow service to remain UP even if dependencies are down
            }
            
            // Add other service checks here as needed
            // Note: External service failures don't affect core service health
            // Circuit breakers and fallbacks ensure service remains operational
            
            // Service core is UP if we reach here
            return builder.up()
                    .withDetail("core-service", "UP")
                    .withDetail("note", "External service dependencies may be unavailable but service is operational")
                    .build();
        } catch (Exception e) {
            log.error("Error checking service health", e);
            return builder.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

