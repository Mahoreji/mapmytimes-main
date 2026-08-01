package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequest {
    private List<String> userIds;
    private String subject;
    private String message;
    private String notificationType;
    private String channel; // EMAIL, SMS, PUSH
}