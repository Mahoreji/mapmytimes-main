package in.mapmytour.blog.dto.response.section;

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
public class SectionResponse {

    private String id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private String accentColor;
    private Integer sortOrder;
    private String parentSectionId;
    private List<SectionResponse> subSections;
    private Integer postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
