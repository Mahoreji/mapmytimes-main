package in.mapmytour.blog.dto.request.readingprogress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertReaderPrefsRequest {

    private Integer fontSizeIdx;
    private String fontStack;
    private String theme;
    private String lineSpacing;
}
