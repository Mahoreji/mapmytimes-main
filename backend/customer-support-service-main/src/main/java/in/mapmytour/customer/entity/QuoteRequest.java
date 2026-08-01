package in.mapmytour.customer.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "quote_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequest {

    @Id
    private String id;

    @Embedded
    private PersonalInfo personalInfo;

    @Embedded
    private TripDetails tripDetails;

    @Embedded
    private AccommodationPreferences accommodationPreferences;

    @Embedded
    private ActivitiesAndInclusions activitiesAndInclusions;

    @Embedded
    private Budget budget;

    @Embedded
    private Consent consent;

    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String status; // "PENDING", "PROCESSING", "COMPLETED", "REJECTED"
    private String assignedAgentId;
    private String quoteResponseId;

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        private String fullName;
        private String email;
        private String phoneNumber;

        @Enumerated(EnumType.STRING)
        private ContactMethod preferredContactMethod;
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripDetails {
        private String destination;
        private String departureCity;
        private LocalDate departureDate;
        private LocalDate returnDate;
        private boolean flexibleDates;
        private int numberOfAdults;
        private int numberOfChildren;

        @Enumerated(EnumType.STRING)
        private TravelType travelType;
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccommodationPreferences {
        @Enumerated(EnumType.STRING)
        private HotelCategory hotelCategory;
        private int numberOfRooms;

        @Enumerated(EnumType.STRING)
        private RoomType roomType;
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivitiesAndInclusions {
        @ElementCollection
        @CollectionTable(name = "quote_request_activities", joinColumns = @JoinColumn(name = "quote_request_id"))
        @Column(name = "activity")
        private List<String> interestedActivities;
        private boolean needGuide;
        private boolean includeFlights;
        private boolean includeMeals;
        private String specialRequests;
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Budget {
        private String estimatedBudget;
        private boolean isBudgetFlexible;
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Consent {
        private boolean acceptTerms;
        private boolean subscribeNewsletter;
    }

    public enum ContactMethod {
        EMAIL, PHONE, WHATSAPP
    }

    public enum TravelType {
        FAMILY, HONEYMOON, SOLO, GROUP, CORPORATE
    }

    public enum HotelCategory {
        BUDGET, STANDARD, LUXURY, ANY
    }

    public enum RoomType {
        SINGLE, DOUBLE, SUITE
    }
}