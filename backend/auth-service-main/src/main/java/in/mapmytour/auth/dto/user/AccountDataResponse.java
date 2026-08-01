
package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDataResponse {
    private Map<String, Object> data;
    private LocalDateTime exportDate;
    private String format;
    private String downloadUrl;
    private Long dataSize;
}