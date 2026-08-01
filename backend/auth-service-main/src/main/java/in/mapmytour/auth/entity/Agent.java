package in.mapmytour.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String agentCode;

    @Column(nullable = false)
    private String agencyName;

    @Column(nullable = false)
    private String contactPerson;

    private String phone;

    private String alternatePhone;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String pincode;

    // GST Details
    private String gstin;
    private String gstRegistrationType;
    private String gstStateCode;
    private String gstLegalName;
    private String gstTradeName;
    private String gstJurisdiction;
    private String gstRegistrationDate;

    // PAN Details
    private String pan;
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
    private String businessType;
    private String businessCategory;
    private String annualTurnover;
    private String yearsInBusiness;
    private String numberOfEmployees;
    private String website;
    @Column(columnDefinition = "TEXT")
    private String businessDescription;

    // Contact Details
    private String alternateEmail;
    private String fax;
    private String landline;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
