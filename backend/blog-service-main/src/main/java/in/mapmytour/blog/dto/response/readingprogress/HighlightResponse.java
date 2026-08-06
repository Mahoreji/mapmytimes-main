package in.mapmytour.blog.dto.response.readingprogress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HighlightResponse {

    private String id;
    private String postId;
    private int paragraphIndex;
    private int charStart;
    private int charEnd;
    private String excerpt;
    private LocalDateTime createdAt;
}
