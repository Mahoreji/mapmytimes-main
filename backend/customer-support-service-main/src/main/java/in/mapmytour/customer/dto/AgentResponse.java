package in.mapmytour.customer.dto;

import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    private String id;
    private String userId;
    private String fullName;
    private String email;
    private Set<String> skills;
    private boolean isActive;
    private Integer maxActiveTickets;
}