package in.mapmytour.auth.dto.auth;

import in.mapmytour.auth.dto.client.CreateSupplierRequest.SupplierType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterSupplierRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    private String userAgent;

    private String ipAddress;

    private boolean agreeToTerms;

    // Supplier specific fields
    @NotBlank(message = "Supplier code is required")
    private String supplierCode;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Contact person is required")
    private String contactPerson;

    private String alternatePhone;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    private String gstin;
       private String pan;

    private String gstRegistrationType;
    private String gstStateCode;
    private String gstLegalName;
    private String gstTradeName;
    private String gstJurisdiction;
    private String gstRegistrationDate;

    private String panLegalName;
    private String panFatherName;
    private String panDateOfBirth;

    private String incorporationNumber;
    private String incorporationType;
    private String incorporationDate;

    private String registrationAuthority;
    private String registrationState;

    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
    private String bankBranch;
    private String bankAccountType;
    private String bankAccountHolderName;
    private String bankCity;
    private String bankState;

    private String supplierType; // Note: In AuthController it passes a string SupplierType instead of
                                 // SupplierType enum. Need to check type.
    private String businessCategory;
    private String annualTurnover;
    private String yearsInBusiness;
    private String numberOfEmployees;
    private String website;
    private String businessDescription;
    private String alternateEmail;
    private String fax;
    private String landline;
}
