package in.mapmytour.auth.dto.user;

import in.mapmytour.auth.entity.PollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CirclePollResponse {

    private String id;
    private String circleId;
    private String question;
    private PollStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime closesAt;
    private List<CirclePollOptionResponse> options;
}
