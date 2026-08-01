package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CirclePollOptionResponse {
    private String id;
    private String text;
    private long voteCount;
    private boolean selectedByCurrentUser;
}
