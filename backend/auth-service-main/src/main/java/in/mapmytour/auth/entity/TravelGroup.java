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
@Table(name = "travel_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 200)
    private String name; // Group name

    @Column(length = 500)
    private String description;

    /**
     * Optional group image / avatar URL shown in UI.
     * Frontend can upload to storage (e.g. S3) and store the URL here.
     */
    @Column(length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy; // Group creator/admin

    @Column(nullable = false, length = 200)
    private String destination; // Travel destination

    @Column(nullable = false)
    private LocalDate travelDate;

    @Column
    private LocalDate returnDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxMembers = 10;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentMembers = 1; // Starts with creator

    @Column(length = 50)
    @Builder.Default
    private String status = "PLANNING"; // PLANNING, CONFIRMED, ONGOING, COMPLETED, CANCELLED

    @Column(length = 50)
    private String travelType; // SOLO, GROUP, FAMILY, COUPLE

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = true; // Public or private group

    @Column(length = 200)
    private String inviteCode; // Unique code for joining

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

