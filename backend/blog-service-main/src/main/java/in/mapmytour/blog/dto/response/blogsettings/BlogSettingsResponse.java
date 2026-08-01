package in.mapmytour.blog.dto.response.blogsettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogSettingsResponse {

    private String id;
    private String settingKey;
    private String settingValue;
}
