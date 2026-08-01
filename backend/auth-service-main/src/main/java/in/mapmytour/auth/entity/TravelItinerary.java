package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "travel_itineraries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private TravelGroup group; // Optional: if part of a travel group

    @Column(nullable = false, length = 200)
    private String title; // e.g., "Day 1 in Manali"

    @Column(nullable = false)
    private LocalDate itineraryDate;

    @Column
    private LocalTime startTime;

    @Column
    private LocalTime endTime;

    @Column(length = 200)
    private String activityType; // SIGHTSEEING, FOOD, ADVENTURE, SHOPPING, REST, etc.

    @Column(length = 500)
    private String activityName; // e.g., "Visit Rohtang Pass"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String location; // Specific location/venue

    @Column(length = 500)
    private String notes; // Additional notes

    @Column
    private Double estimatedCost; // Estimated cost for this activity

    @Column(length = 200)
    private String bookingReference; // Hotel/flight/activity booking reference

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0; // Order of activities in the day

    @Column(nullable = false)
    @Builder.Default
    private Boolean isShared = false; // Whether shared with group/connections

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

