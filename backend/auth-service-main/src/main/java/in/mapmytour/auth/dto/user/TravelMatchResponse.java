package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelMatchResponse {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String destination;
    private LocalDate travelDate;
    private LocalDate returnDate;
    private double compatibilityScore; // 0-100
    private String matchReasons; // Why they matched
    private int commonInterests;
    private boolean sameTravelStyle;
    private boolean overlappingDates;
    private String travelType;
}

