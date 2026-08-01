package in.mapmytour.auth.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierAnalyticsResponse {

    // Totals
    private long totalSuppliers;
    private long activeSuppliers;
    private long inactiveSuppliers;
    private long verifiedSuppliers;
    private long unverifiedSuppliers;
    private long pendingVerificationSuppliers;

    // Verification Breakdown
    private long autoVerifiedSuppliers;
    private long manuallyVerifiedSuppliers;

    // Geographic Breakdown
    private Map<String, Long> suppliersByCity;
    private Map<String, Long> suppliersByState;
    private Map<String, Long> suppliersByCountry;

    // Supplier Type Breakdown (Hotels, Transport, etc.)
    private Map<String, Long> suppliersByType;
    private Map<String, Long> suppliersByBusinessCategory;

    // Registration Trend (by month: "2026-01" -> count)
    private Map<String, Long> registrationTrend;

    // GST / PAN coverage
    private long suppliersWithGstin;
    private long suppliersWithPan;
}
