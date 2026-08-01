package in.mapmytour.auth.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    private String recipient;
    private String subject;
    private String body;
    private String type;
    private String source;
    private Long scheduledAt;
    private Map<String, Object> metadata;
}
