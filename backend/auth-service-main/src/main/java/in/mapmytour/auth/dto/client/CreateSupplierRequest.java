package in.mapmytour.auth.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupplierRequest {

    private String supplierCode;
    private String companyName;
    private String contactPerson;
    private String email;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String gstin;
    private String pan;
    private SupplierType type;
    private String status;
    private String riskRating;
    private BigDecimal creditLimit;
    private Integer paymentTerms;
    private Map<String, Object> bankDetails;
    private Map<String, Object> kycDocuments;
    private Map<String, Object> contractDetails;
    private Map<String, Object> metadata;

    public enum SupplierType {
        HOTEL, TRANSPORT, ACTIVITY, VISA, TOUR_PACKAGE, FLIGHT
    }
}
