package in.mapmytour.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    private String email;

    private Set<String> skills;
    private Integer maxActiveTickets;
}