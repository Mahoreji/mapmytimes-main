package in.mapmytour.blog.dto.request.blogpost;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslateRequest {

    @NotEmpty(message = "Items list is required")
    @Size(max = 200, message = "Maximum 200 items allowed per request")
    private List<Item> items;

    @NotBlank(message = "Source language is required")
    private String sourceLang;

    @NotBlank(message = "Target language is required")
    private String targetLang;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        @NotBlank(message = "Item id is required")
        private String id;

        @Size(max = 20000, message = "Single text item too long (max 20000 chars)")
        private String text;
    }
}
