package in.mapmytour.customer.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback implementation for PaymentServiceClient
 * Provides default responses when payment service is unavailable
 */
@Component
@Slf4j
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public Map<String, Object> getPaymentDetails(String paymentId) {
        log.warn("Payment service unavailable, returning fallback for payment: {}", paymentId);
        return createFallbackPaymentResponse(paymentId);
    }

    @Override
    public Map<String, Object> getPaymentByBookingId(String bookingId) {
        log.warn("Payment service unavailable, returning fallback for booking: {}", bookingId);
        return createFallbackPaymentResponse(null);
    }

    @Override
    public Map<String, Object> getPaymentStatus(String paymentId) {
        log.warn("Payment service unavailable, returning fallback status for payment: {}", paymentId);
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", paymentId);
        response.put("status", "UNKNOWN");
        response.put("message", "Payment service unavailable");
        return response;
    }

    private Map<String, Object> createFallbackPaymentResponse(String paymentId) {
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", paymentId);
        response.put("status", "UNKNOWN");
        response.put("amount", null);
        response.put("currency", null);
        response.put("message", "Payment service unavailable");
        return response;
    }
}

