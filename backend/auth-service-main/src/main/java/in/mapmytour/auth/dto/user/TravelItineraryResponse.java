package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelItineraryResponse {
    private String id;
    private String userId;
    private String groupId;
    private String title;
    private LocalDate itineraryDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String activityType;
    private String activityName;
    private String description;
    private String location;
    private String notes;
    private Double estimatedCost;
    private String bookingReference;
    private Integer orderIndex;
    private Boolean isShared;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

