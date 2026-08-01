package in.mapmytour.blog.dto.request.tag;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTagRequest {

    @NotBlank(message = "Tag name is required")
    private String name;

    @NotBlank(message = "Tag slug is required")
    private String slug;
}
