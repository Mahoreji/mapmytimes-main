package in.mapmytour.blog.dto.request.readingprogress;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertProgressRequest {

    @NotBlank(message = "Post ID is required")
    private String postId;

    @Min(value = 0, message = "Scroll percent must be between 0 and 100")
    @Max(value = 100, message = "Scroll percent must be between 0 and 100")
    private int scrollPercent;
}
