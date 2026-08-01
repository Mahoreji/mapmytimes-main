package in.mapmytour.auth.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterAgentRequest {

    // ============ BASIC INFORMATION (Required for User Creation) ============
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must contain at least one digit, one lowercase, one uppercase letter and one special character")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @NotNull(message = "You must agree to terms and conditions")
    @AssertTrue(message = "You must agree to terms and conditions")
    private Boolean agreeToTerms;

    // ============ AGENT-SPECIFIC FIELDS (Optional - stored for agent service) ============
    
    private String alternatePhone;
    
    @NotBlank(message = "Agent code is required")
    @Pattern(regexp = "^[A-Z0-9]{3,50}$", message = "Agent code must be alphanumeric uppercase")
    private String agentCode;

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    @NotBlank(message = "Contact person is required")
    @Size(max = 255, message = "Contact person name must not exceed 255 characters")
    private String contactPerson;

    // ============ ADDRESS INFORMATION ============
    
    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    // ============ GST DETAILS (INDIAN) ============
    
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", 
               message = "Invalid GSTIN format (15 characters: 2-digit state code + 10-digit PAN + 3 characters)")
    private String gstin;

    private String gstRegistrationType; // REGULAR, COMPOSITION, UNREGISTERED
    private String gstStateCode; // 2-digit state code
    private String gstLegalName; // Legal name as per GST certificate
    private String gstTradeName; // Trade name if different from legal name
    private String gstJurisdiction; // GST jurisdiction (State/Central)
    private String gstRegistrationDate; // Date of GST registration (YYYY-MM-DD)

    // ============ PAN CARD DETAILS ============
    
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format (10 characters)")
    private String pan;

    private String panLegalName; // Name as per PAN card
    private String panFatherName; // Father's name as per PAN card
    private String panDateOfBirth; // Date of birth as per PAN (YYYY-MM-DD)

    // ============ INCORPORATION DETAILS ============
    
    private String incorporationNumber; // CIN (Company Identification Number) for companies
    private String incorporationType; // PRIVATE_LIMITED, PUBLIC_LIMITED, LLP, PARTNERSHIP, PROPRIETORSHIP, etc.
    private String incorporationDate; // Date of incorporation (YYYY-MM-DD)
    private String registrationAuthority; // ROC (Registrar of Companies), etc.
    private String registrationState; // State of registration

    // ============ BANK DETAILS ============
    
    @NotBlank(message = "Bank account number is required")
    private String bankAccountNumber;
    
    @NotBlank(message = "Bank IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    private String bankIfscCode;
    
    @NotBlank(message = "Bank name is required")
    private String bankName;
    
    @NotBlank(message = "Bank branch is required")
    private String bankBranch;
    
    private String bankAccountType; // SAVINGS, CURRENT, OD (Overdraft)
    private String bankAccountHolderName; // Account holder name as per bank records
    private String bankCity;
    private String bankState;

    // ============ BUSINESS DETAILS ============
    
    private String businessType; // B2B_TRAVEL_AGENT, TOUR_OPERATOR, DMC, etc.
    private String businessCategory; // DOMESTIC, INTERNATIONAL, BOTH
    private String annualTurnover; // Estimated annual turnover
    private String yearsInBusiness; // Number of years in business
    private String numberOfEmployees; // Approximate number of employees
    private String website; // Company website URL
    private String businessDescription; // Brief description of business

    // ============ CONTACT DETAILS ============
    
    private String alternateEmail;
    private String fax;
    private String landline;

    // ============ VALIDATION METHODS ============
    
    @AssertTrue(message = "Passwords must match")
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    private String ipAddress;
    private String userAgent;
}

