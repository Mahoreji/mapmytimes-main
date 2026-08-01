package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryResponse {
    private List<LoginHistoryItemResponse> loginHistory;
    private Long totalLogins;
    private int page;
    private int size;
}