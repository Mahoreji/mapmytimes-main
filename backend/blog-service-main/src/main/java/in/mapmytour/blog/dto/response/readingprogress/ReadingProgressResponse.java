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
public class ReadingProgressResponse {

    private String postId;
    private int scrollPercent;
    private LocalDateTime updatedAt;
}
