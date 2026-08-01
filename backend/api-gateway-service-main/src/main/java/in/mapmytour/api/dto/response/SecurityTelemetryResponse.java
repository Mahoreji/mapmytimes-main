package in.mapmytour.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * DTO for real-time security telemetry.
 */
@Data
@Builder
public class SecurityTelemetryResponse {
    private long totalBlockedRequests;
    private long activeBotBans;
    private long whitelistedIpCount;
    private List<AttackerInfo> topAttackers;
    private Map<String, Long> eventsByAction;
    private String systemStatus; // HEALTHY, DEGRADED, CRITICAL
    private String databaseStatus;
    private String redisStatus;

    @Data
    @Builder
    public static class AttackerInfo {
        private String ip;
        private int threatScore;
        private String lastSeen;
        private String lastAction;
    }
}
