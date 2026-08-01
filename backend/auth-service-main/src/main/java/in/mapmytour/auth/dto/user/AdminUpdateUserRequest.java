package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private Boolean isActive;
    private Boolean isVerified;
    private String notes;
}