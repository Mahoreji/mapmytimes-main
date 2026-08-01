package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActivityItemResponse {
    private String id;
    private String userEmail;
    private String userId;
    private String activityType;
    private String description;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private String status;
    private String metadata; // JSON string containing package info, page views, clicks, etc.
}

