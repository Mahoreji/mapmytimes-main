package in.mapmytour.auth.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AssignRolesRequest {

    @Email
    @NotBlank
    private String userEmail;

    /**
     * Role names to assign, e.g. ["ACCOUNTING_MANAGER", "ACCOUNTING_VIEWER"]
     */
    @NotEmpty
    private Set<String> roles;
}


