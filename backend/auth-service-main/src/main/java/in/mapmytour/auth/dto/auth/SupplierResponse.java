package in.mapmytour.auth.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponse {
    private String id;
    private String supplierCode;
    private String companyName;
    private String contactPerson;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;

    // GST Details
    private String gstin;
    private String pan;
    private String gstRegistrationType;
    private String gstStateCode;
    private String gstLegalName;
    private String gstTradeName;
    private String gstJurisdiction;
    private String gstRegistrationDate;

    // PAN Details
    private String panLegalName;
    private String panFatherName;
    private String panDateOfBirth;

    // Incorporation Details
    private String incorporationNumber;
    private String incorporationType;
    private String incorporationDate;
    private String registrationAuthority;
    private String registrationState;

    // Bank Details
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
    private String bankBranch;
    private String bankAccountType;
    private String bankAccountHolderName;
    private String bankCity;
    private String bankState;

    // Business Details
    private String supplierType;
    private String businessCategory;
    private String annualTurnover;
    private String yearsInBusiness;
    private String numberOfEmployees;
    private String website;
    private String businessDescription;

    // Contact Details
    private String alternateEmail;
    private String fax;
    private String landline;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
