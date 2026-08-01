package in.mapmytour.blog.dto.response.blogpost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslateResponse {
    private String sourceLang;
    private String targetLang;
    private List<Item> items;
    private String disclaimer;
    private Boolean translated;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String id;
        private String source;
        private String translated;
    }
}
