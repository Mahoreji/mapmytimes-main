package in.mapmytour.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailsDTO {
    
    private String bookingId;
    private String bookingReference;
    private String customerId;
    private String customerName;
    private String customerEmail;
    
    private String destination;
    private LocalDate travelDate;
    private LocalDate returnDate;
    private Integer numberOfTravelers;
    
    private BigDecimal totalAmount;
    private String currency;
    private String paymentStatus;
    private String bookingStatus;
    
    private LocalDateTime bookingDate;
    private LocalDateTime lastModified;
    
    // Additional booking details
    private String hotelName;
    private String flightDetails;
    private String specialRequests;
}

