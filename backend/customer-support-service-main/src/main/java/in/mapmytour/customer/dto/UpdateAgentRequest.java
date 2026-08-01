package in.mapmytour.customer.dto;

import lombok.*;

import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgentRequest {

    private String fullName;
    private Set<String> skills;
    private Boolean isActive;
    private Integer maxActiveTickets;
}