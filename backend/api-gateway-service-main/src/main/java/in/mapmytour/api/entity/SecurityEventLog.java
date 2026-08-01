package in.mapmytour.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * R2DBC entity representing a single gateway security event.
 * Persisted asynchronously after every security action (block, ban, rate-limit...).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("gateway_security_events")
public class SecurityEventLog {

    @Id
    private Long id;

    @Column("event_time")
    private LocalDateTime eventTime;

    @Column("client_ip")
    private String clientIp;

    @Column("user_id")
    private String userId;

    @Column("endpoint")
    private String endpoint;

    @Column("method")
    private String method;

    @Column("action")
    private String action;     // BOT_DETECTED, RATE_LIMIT_EXCEEDED, AUTH_FAILURE, ADMIN_ACCESS…

    @Column("result")
    private String result;     // BLOCKED, ALLOWED, THROTTLED, DENIED

    @Column("threat_score")
    @Builder.Default
    private int threatScore = 0;

    @Column("details")
    private String details;
}
