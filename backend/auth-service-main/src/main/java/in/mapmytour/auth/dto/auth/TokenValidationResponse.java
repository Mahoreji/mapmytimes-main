package in.mapmytour.auth.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private boolean expired;
    private String tokenType;
    private String username;
    private String userId;
    private List<String> roles;
    private LocalDateTime expiresAt;
    private String error;
    private Map<String, Object> metadata;
}