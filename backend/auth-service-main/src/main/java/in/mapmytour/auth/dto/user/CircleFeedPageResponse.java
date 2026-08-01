package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Simple wrapper for paginated circle posts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleFeedPageResponse {

    private List<CirclePostResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private boolean last;
}
