package in.mapmytour.customer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign Client for Payment Service Integration
 * Handles payment-related queries for support tickets
 */
@FeignClient(
    name = "payment-service",
    url = "${payment.service.url:http://payment-service:8088}",
    fallback = PaymentServiceClientFallback.class
)
public interface PaymentServiceClient {

    /**
     * Get payment details by payment ID
     * @param paymentId The payment ID
     * @return Payment details
     */
    @GetMapping("/api/v1/payments/{paymentId}")
    Map<String, Object> getPaymentDetails(@PathVariable("paymentId") String paymentId);

    /**
     * Get payment details by booking ID
     * @param bookingId The booking ID
     * @return Payment details
     */
    @GetMapping("/api/v1/payments/booking/{bookingId}")
    Map<String, Object> getPaymentByBookingId(@PathVariable("bookingId") String bookingId);

    /**
     * Get payment status
     * @param paymentId The payment ID
     * @return Payment status
     */
    @GetMapping("/api/v1/payments/{paymentId}/status")
    Map<String, Object> getPaymentStatus(@PathVariable("paymentId") String paymentId);
}

