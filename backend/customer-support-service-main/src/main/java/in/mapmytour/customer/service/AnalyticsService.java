package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.AnalyticsResponse;
import in.mapmytour.customer.dto.AgentPerformanceMetrics;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for performance metrics and analytics
 */
public interface AnalyticsService {
    
    /**
     * Get overall analytics dashboard data
     */
    AnalyticsResponse getAnalytics(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get agent performance metrics
     */
    List<AgentPerformanceMetrics> getAgentPerformanceMetrics(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get SLA compliance metrics
     */
    AnalyticsResponse.SLAMetrics getSLAMetrics(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get ticket volume trends
     */
    List<AnalyticsResponse.TicketVolumeTrend> getTicketVolumeTrends(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get category distribution
     */
    List<AnalyticsResponse.CategoryDistribution> getCategoryDistribution(LocalDateTime startDate, LocalDateTime endDate);
}

