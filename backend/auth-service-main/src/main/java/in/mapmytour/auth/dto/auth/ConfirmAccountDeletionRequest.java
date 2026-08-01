package in.mapmytour.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAccountDeletionRequest {

    @NotBlank(message = "Deletion token is required")
    private String deletionToken;

    @NotBlank(message = "Password is required")
    private String password;
}