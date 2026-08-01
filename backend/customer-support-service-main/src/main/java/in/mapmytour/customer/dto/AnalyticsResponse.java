package in.mapmytour.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    
    private Integer totalTickets;
    private Integer openTickets;
    private Integer inProgressTickets;
    private Integer resolvedTickets;
    private Integer closedTickets;
    
    private Double averageResponseTimeMinutes;
    private Double averageResolutionTimeMinutes;
    
    private SLAMetrics slaMetrics;
    private List<TicketVolumeTrend> ticketVolumeTrends;
    private List<CategoryDistribution> categoryDistribution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SLAMetrics {
        private Integer totalTickets;
        private Integer responseSLAMet;
        private Integer responseSLABreached;
        private Integer resolutionSLAMet;
        private Integer resolutionSLABreached;
        private Double responseSLAPercentage;
        private Double resolutionSLAPercentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketVolumeTrend {
        private String date;
        private Integer ticketCount;
        private Integer resolvedCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDistribution {
        private String category;
        private Integer count;
        private Double percentage;
    }
}

