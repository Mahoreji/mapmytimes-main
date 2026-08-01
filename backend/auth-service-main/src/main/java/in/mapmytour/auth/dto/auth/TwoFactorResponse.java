package in.mapmytour.auth.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorResponse {
    private boolean verified;
    private String message;
    private String qrCodeUrl;
    private String secret;
    private List<String> backupCodes;
}