package in.mapmytour.blog.dto.response.tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagResponse {

    private String id;
    private String name;
    private String slug;
    private Integer postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
