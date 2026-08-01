package in.mapmytour.blog.dto.request.blogsettings;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSettingRequest {

    @NotBlank(message = "Setting key is required")
    private String settingKey;

    private String settingValue;
}
