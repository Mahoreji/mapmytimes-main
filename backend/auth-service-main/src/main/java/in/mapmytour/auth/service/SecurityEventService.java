package in.mapmytour.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SecurityEventService {

    public enum SecurityEventType {
        OAUTH2_LOGIN_SUCCESS,
        OAUTH2_LOGIN_FAILED,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        PASSWORD_RESET,
        ACCOUNT_LOCKED
    }

    public void logSecurityEvent(String userEmail, SecurityEventType eventType, String ipAddress, String userAgent) {
        log.info("Security Event - User: {}, Event: {}, IP: {}, UserAgent: {}",
                userEmail, eventType, ipAddress, userAgent);

        // Here you can add logic to:
        // - Store events in database
        // - Send alerts for suspicious activities
        // - Integrate with monitoring systems
    }
}