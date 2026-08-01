package in.mapmytour.auth.dto.analytics;

import in.mapmytour.auth.entity.Supplier;
import in.mapmytour.auth.entity.VerificationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDetailResponse {
    private Supplier supplier;
    private List<VerificationRequest> verificationHistory;
}
