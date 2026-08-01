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
public class AgentAnalyticsResponse {

    // Totals
    private long totalAgents;
    private long activeAgents;
    private long inactiveAgents;
    private long verifiedAgents;
    private long unverifiedAgents;
    private long pendingVerificationAgents;

    // Verification Breakdown
    private long autoVerifiedAgents;
    private long manuallyVerifiedAgents;

    // Geographic Breakdown
    private Map<String, Long> agentsByCity;
    private Map<String, Long> agentsByState;
    private Map<String, Long> agentsByCountry;

    // Business Breakdown
    private Map<String, Long> agentsByBusinessType;
    private Map<String, Long> agentsByBusinessCategory;

    // Registration Trend (by month: "2026-01" -> count)
    private Map<String, Long> registrationTrend;

    // GST / PAN coverage
    private long agentsWithGstin;
    private long agentsWithPan;
}
