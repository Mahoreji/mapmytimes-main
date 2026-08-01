package in.mapmytour.auth.dto.user;

import in.mapmytour.auth.entity.CircleVisibility;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request to create a new trip circle.
 */
@Data
public class TripCircleCreateRequest {

    @NotBlank
    private String destinationId;

    private String title;

    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private CircleVisibility visibility = CircleVisibility.DESTINATION_PUBLIC;
}
