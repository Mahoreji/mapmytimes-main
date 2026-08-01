package in.mapmytour.auth.dto.analytics;

import in.mapmytour.auth.entity.Agent;
import in.mapmytour.auth.entity.VerificationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentDetailResponse {
    private Agent agent;
    private List<VerificationRequest> verificationHistory;
}
