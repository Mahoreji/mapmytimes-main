package in.mapmytour.auth.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminResetPasswordRequest {

    @NotBlank(message = "User email is required")
    @Email(message = "Please provide a valid email address")
    private String userEmail;

    private String newPassword;
    private boolean sendEmail;
}