package in.mapmytour.customer.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequestDTO {

    @NotNull
    private PersonalInfo personalInfo;

    @NotNull
    private TripDetails tripDetails;

    @NotNull
    private AccommodationPreferences accommodationPreferences;

    @NotNull
    private ActivitiesAndInclusions activitiesAndInclusions;

    @NotNull
    private Budget budget;

    @NotNull
    private Consent consent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        @NotBlank
        private String fullName;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Pattern(regexp = "^[0-9]{10,15}$")
        private String phoneNumber;

        @NotNull
        private ContactMethod preferredContactMethod;
    }

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
        private TravelType travelType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccommodationPreferences {
        @NotNull
        private HotelCategory hotelCategory;

        @Min(1)
        private int numberOfRooms;

        @NotNull
        private RoomType roomType;
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