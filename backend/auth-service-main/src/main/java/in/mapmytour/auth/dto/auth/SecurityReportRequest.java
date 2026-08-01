package in.mapmytour.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityReportRequest {

    @NotBlank(message = "Description is required")
    private String description;

    private String eventType;
    private String ipAddress;
    private String userAgent;
    private String additionalInfo;
}