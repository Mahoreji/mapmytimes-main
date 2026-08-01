package in.mapmytour.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponseDTO {
    private String id;
    private PersonalInfo personalInfo;
    private TripDetails tripDetails;
    private AccommodationPreferences accommodationPreferences;
    private ActivitiesAndInclusions activitiesAndInclusions;
    private Budget budget;
    private Consent consent;
    private LocalDate createdAt;
    private String status;
    private String assignedAgentId;
    private String quoteResponseId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripDetails {
        @NotBlank
        private String destination;

        @NotBlank
        private String departureCity;

        @NotNull
        @FutureOrPresent
        private LocalDate departureDate;

        @NotNull
        @Future
        private LocalDate returnDate;

        private boolean flexibleDates;

        @Min(1)
        private int numberOfAdults;

        @Min(0)
        private int numberOfChildren;

        @NotNull
        private QuoteRequestDTO.TravelType travelType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        private String fullName;
        private String email;
        private String phoneNumber;
        private QuoteRequestDTO.ContactMethod preferredContactMethod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccommodationPreferences {
        @NotNull
        private QuoteRequestDTO.HotelCategory hotelCategory;

        @Min(1)
        private int numberOfRooms;

        @NotNull
        private QuoteRequestDTO.RoomType roomType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivitiesAndInclusions {
        private List<String> interestedActivities;
        private boolean needGuide;
        private boolean includeFlights;
        private boolean includeMeals;
        private String specialRequests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Budget {
        private String estimatedBudget;
        private boolean isBudgetFlexible;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Consent {
        @AssertTrue
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

