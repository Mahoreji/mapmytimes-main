package in.mapmytour.auth.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalAgents;
    private long totalSuppliers;
    private long pendingVerifications;

    private Map<String, Long> userRegistrationTrend; // Combined trend
    private Map<String, Long> verificationStatusDistribution; // PENDING, APPROVED, REJECTED

    private long activeUsers;
    private long inactiveUsers;
}
