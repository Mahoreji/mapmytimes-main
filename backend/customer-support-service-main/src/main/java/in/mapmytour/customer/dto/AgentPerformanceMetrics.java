package in.mapmytour.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPerformanceMetrics {
    
    private String agentId;
    private String agentName;
    private String agentEmail;
    
    private Integer totalTicketsAssigned;
    private Integer ticketsResolved;
    private Integer ticketsOpen;
    
    private Double averageResponseTimeMinutes;
    private Double averageResolutionTimeMinutes;
    
    private Integer slaResponseMet;
    private Integer slaResponseBreached;
    private Integer slaResolutionMet;
    private Integer slaResolutionBreached;
    
    private Double customerSatisfactionRating;
    private Integer totalFeedbacks;
}

