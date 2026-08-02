package in.mapmytour.blog.dto.request.section;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSectionRequest {

    @NotBlank(message = "Section name is required")
    private String name;

    @NotBlank(message = "Section slug is required")
    private String slug;

    private String description;
    private String icon;
    private String accentColor;
    private Integer sortOrder;
    private String parentSectionId;
}
