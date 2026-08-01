package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    private String type;
    private String channel; // EMAIL, SMS, PUSH, ALL
    private String title;
    private String message;
    private String templateName;
    private Map<String, Object> templateVariables;
    private Map<String, Object> metadata;
    private boolean urgent;
    private String scheduledAt;
}