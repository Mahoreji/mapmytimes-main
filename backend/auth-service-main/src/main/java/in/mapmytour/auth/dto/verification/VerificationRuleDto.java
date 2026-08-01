package in.mapmytour.auth.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import in.mapmytour.auth.entity.VerificationRule.AutomationType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationRuleDto {
    private String id;

    @NotBlank(message = "Role type is required")
    private String roleType;

    @NotBlank(message = "Field name is required")
    private String fieldName;

    @NotNull(message = "isMandatory flag is required")
    private Boolean isMandatory;

    @NotNull(message = "isAutomated flag is required")
    private Boolean isAutomated;

    private AutomationType automationType;

    private String description;
}
