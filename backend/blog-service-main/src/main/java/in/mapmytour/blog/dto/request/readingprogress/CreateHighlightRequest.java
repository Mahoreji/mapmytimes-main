package in.mapmytour.blog.dto.request.readingprogress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHighlightRequest {

    @NotBlank(message = "Post ID is required")
    private String postId;

    @NotNull(message = "Paragraph index is required")
    private Integer paragraphIndex;

    @NotNull(message = "Char start is required")
    private Integer charStart;

    @NotNull(message = "Char end is required")
    private Integer charEnd;

    @NotBlank(message = "Excerpt is required")
    @Size(max = 200, message = "Excerpt must be at most 200 characters")
    private String excerpt;
}
