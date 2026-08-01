package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanResponse {
    private String id;
    private String userId;
    private String userEmail;
    private String userName;
    private String destination;
    private String description;
    private LocalDate travelDate;
    private LocalDate returnDate;
    private String status;
    private String travelType;
    private Integer numberOfTravelers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

