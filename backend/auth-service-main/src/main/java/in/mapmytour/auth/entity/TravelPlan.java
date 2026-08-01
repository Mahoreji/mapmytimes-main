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

@Entity
@Table(name = "travel_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String destination; // e.g., "Manali", "Goa", "Shimla"

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDate travelDate; // When they plan to travel

    @Column
    private LocalDate returnDate; // Optional return date

    @Column(length = 50)
    @Builder.Default
    private String status = "PLANNED"; // PLANNED, ONGOING, COMPLETED, CANCELLED

    @Column(length = 50)
    private String travelType; // SOLO, GROUP, FAMILY, COUPLE

    @Column
    private Integer numberOfTravelers;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

