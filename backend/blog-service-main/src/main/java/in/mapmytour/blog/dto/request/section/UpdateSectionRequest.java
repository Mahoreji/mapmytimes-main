package in.mapmytour.blog.dto.request.section;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSectionRequest {

    private String name;
    private String slug;
    private String description;
    private String icon;
    private String accentColor;
    private Integer sortOrder;
    private String parentSectionId;
}
