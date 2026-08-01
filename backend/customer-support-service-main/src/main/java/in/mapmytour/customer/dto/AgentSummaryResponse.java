package in.mapmytour.customer.dto;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSummaryResponse {

    private String id;
    private String fullName;
    private String email;
    private boolean isActive;
}