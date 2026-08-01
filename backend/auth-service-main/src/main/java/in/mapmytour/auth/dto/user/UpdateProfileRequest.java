package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Update Profile Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Pattern(regexp = "^(\\+\\d{1,3}[- ]?)?\\d{10}$", message = "Please provide a valid phone number")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(male|female|other)$", message = "Gender must be male, female, or other")
    private String gender;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio; // User biography/description

    private String coverImageUrl; // Profile cover image URL

    private AddressRequest address;
    private UserPreferencesRequest preferences;
}
