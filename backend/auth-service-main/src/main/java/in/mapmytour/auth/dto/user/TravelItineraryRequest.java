package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelItineraryRequest {
    private String groupId; // Optional: if part of a travel group

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Itinerary date is required")
    private LocalDate itineraryDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private String activityType; // SIGHTSEEING, FOOD, ADVENTURE, SHOPPING, REST, etc.
    private String activityName;
    private String description;
    private String location;
    private String notes;
    private Double estimatedCost;
    private String bookingReference;
    private Integer orderIndex;
    private Boolean isShared;
}

