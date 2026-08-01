package in.mapmytour.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class RoleRequest {

    @NotBlank
    private String name; // e.g. ACCOUNTING_MANAGER

    private String description;

    /**
     * Permission codes to be attached to this role.
     * Example: ["ACCOUNTING_INVOICE_READ", "ACCOUNTING_INVOICE_WRITE"]
     */
    private Set<String> permissionCodes;
}


