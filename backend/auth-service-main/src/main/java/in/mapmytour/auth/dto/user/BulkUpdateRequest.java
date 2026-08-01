package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateRequest {
    private List<String> userIds;
    private Map<String, Object> updates;
    private String reason;
}