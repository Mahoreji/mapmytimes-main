package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminVerificationActionRequest {
    
    @NotBlank(message = "Action is required (APPROVE or REJECT)")
    private String action; // APPROVE or REJECT
    
    private String adminNotes; // Optional notes from admin
}

