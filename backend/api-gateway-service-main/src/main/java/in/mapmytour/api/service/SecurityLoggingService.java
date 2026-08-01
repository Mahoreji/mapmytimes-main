package in.mapmytour.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.mapmytour.api.entity.SecurityEventLog;
import in.mapmytour.api.repository.SecurityEventRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Centralized security event logging service.
 *
 * All security incidents are:
 *   1. Logged as structured JSON to stdout/files (ELK compatible)
 *   2. Persisted to the database audit table (Security auditing)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityLoggingService {

    private final ObjectMapper objectMapper;
    private final SecurityEventRepository securityEventRepository;

    // =====================================================================
    // Security Event DTO (for JSON logging)
    // =====================================================================

    @Data
    @Builder
    public static class SecurityEvent {
        private String timestamp;
        private String clientIp;
        private String userId;
        private String endpoint;
        private String method;
        private String action;
        private String result;
        private int threatScore;
        private String details;
    }

    // =====================================================================
    // Core Logging Entry-Point
    // =====================================================================

    @Async
    public void logEvent(String clientIp, String userId, String endpoint,
                         String method, String action, String result,
                         int threatScore, String details) {
        
        Instant now = Instant.now();
        
        try {
            // 1. Log to JSON (Middleware/ELK path)
            SecurityEvent event = SecurityEvent.builder()
                    .timestamp(DateTimeFormatter.ISO_INSTANT.format(now))
                    .clientIp(clientIp)
                    .userId(userId != null ? userId : "anonymous")
                    .endpoint(endpoint)
                    .method(method != null ? method : "UNKNOWN")
                    .action(action)
                    .result(result)
                    .threatScore(threatScore)
                    .details(details)
                    .build();

            String json = objectMapper.writeValueAsString(event);
            log.warn("SECURITY_EVENT {}", json);

            // 2. Persist to Database (Persistent Audit path)
            SecurityEventLog dbLog = SecurityEventLog.builder()
                    .eventTime(LocalDateTime.ofInstant(now, ZoneId.systemDefault()))
                    .clientIp(clientIp)
                    .userId(userId)
                    .endpoint(endpoint)
                    .method(method)
                    .action(action)
                    .result(result)
                    .threatScore(threatScore)
                    .details(details)
                    .build();
            
            securityEventRepository.save(dbLog).subscribe();
            
        } catch (Exception e) {
            log.error("Failed to process security event: action={}, ip={}", action, clientIp, e);
        }
    }

    // =====================================================================
    // Convenience Methods
    // =====================================================================

    public void logBlockedRequest(String clientIp, String endpoint, String method, String details, int threatScore) {
        logEvent(clientIp, null, endpoint, method, "ACCESS_BLOCKED", "DENIED", threatScore, details);
    }

    public void logRateLimitViolation(String clientIp, String endpoint, String method) {
        logEvent(clientIp, null, endpoint, method, "RATE_LIMIT_EXCEEDED", "THROTTLED", 0, "Too many requests");
    }

    public void logBotDetection(String clientIp, String endpoint, String method, String userAgent) {
        logEvent(clientIp, null, endpoint, method, "BOT_DETECTED", "BLOCKED", 100,
                "Suspicious User-Agent or scan pattern detected. UA=" + userAgent);
    }

    public void logAdminAccessAttempt(String clientIp, String endpoint, String method, boolean allowed, String userId) {
        logEvent(clientIp, userId, endpoint, method, "ADMIN_ACCESS",
                allowed ? "ALLOWED" : "DENIED", 0,
                allowed ? "IP is whitelisted" : "IP not in admin whitelist");
    }

    public void logAuthFailure(String clientIp, String endpoint, String method, String reason) {
        logEvent(clientIp, null, endpoint, method, "AUTH_FAILURE", "DENIED", 20, reason);
    }

    public void logIpWhitelistChange(String adminIp, String targetIp, String changeType) {
        logEvent(adminIp, null, "/admin/security/ip-whitelist", "POST", "WHITELIST_" + changeType,
                "COMPLETED", 0, "Target IP: " + targetIp);
    }

    public void logThreatScoreEscalation(String clientIp, String endpoint, int score, String action) {
        logEvent(clientIp, null, endpoint, "ANY", "THREAT_SCORE_ESCALATION",
                action, score, "Threat score=" + score + ", action taken=" + action);
    }
}
