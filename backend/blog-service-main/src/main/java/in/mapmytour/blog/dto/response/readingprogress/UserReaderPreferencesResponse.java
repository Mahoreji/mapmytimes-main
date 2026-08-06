package in.mapmytour.blog.dto.response.readingprogress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReaderPreferencesResponse {

    private int fontSizeIdx;
    private String fontStack;
    private String theme;
    private String lineSpacing;
}
