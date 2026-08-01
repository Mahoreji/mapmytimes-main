package in.mapmytour.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteSummaryDTO {
    private String id;
    private String fullName;
    private String email;
    private String destination;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private String status;
    private LocalDate createdAt;
}
