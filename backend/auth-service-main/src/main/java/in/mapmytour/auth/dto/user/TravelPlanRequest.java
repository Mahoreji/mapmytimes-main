package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanRequest {
    @NotBlank(message = "Destination is required")
    private String destination; // e.g., "Manali", "Goa", "Shimla"

    private String description;

    @NotNull(message = "Travel date is required")
    private LocalDate travelDate;

    private LocalDate returnDate;

    private String travelType; // SOLO, GROUP, FAMILY, COUPLE

    private Integer numberOfTravelers;
}

