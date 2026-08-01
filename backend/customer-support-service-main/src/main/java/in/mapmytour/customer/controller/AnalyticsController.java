package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.AgentPerformanceMetrics;
import in.mapmytour.customer.dto.AnalyticsResponse;
import in.mapmytour.customer.dto.APIResponse;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.service.AnalyticsService;
import in.mapmytour.customer.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserContextService userContextService;

    @GetMapping
    public ResponseEntity<APIResponse<AnalyticsResponse>> getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Fetching analytics dashboard");
        
        // Only admin users can view analytics
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException("Only administrators can view analytics");
        }
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        AnalyticsResponse response = analyticsService.getAnalytics(startDate, endDate);
        
        return ResponseEntity.ok(APIResponse.<AnalyticsResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Analytics retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/agents")
    public ResponseEntity<APIResponse<List<AgentPerformanceMetrics>>> getAgentPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Fetching agent performance metrics");
        
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException("Only administrators can view agent performance");
        }
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        List<AgentPerformanceMetrics> response = analyticsService.getAgentPerformanceMetrics(startDate, endDate);
        
        return ResponseEntity.ok(APIResponse.<List<AgentPerformanceMetrics>>builder()
                .success(true)
                .statusCode(200)
                .message("Agent performance metrics retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/sla")
    public ResponseEntity<APIResponse<AnalyticsResponse.SLAMetrics>> getSLAMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Fetching SLA metrics");
        
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException("Only administrators can view SLA metrics");
        }
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        AnalyticsResponse.SLAMetrics response = analyticsService.getSLAMetrics(startDate, endDate);
        
        return ResponseEntity.ok(APIResponse.<AnalyticsResponse.SLAMetrics>builder()
                .success(true)
                .statusCode(200)
                .message("SLA metrics retrieved successfully")
                .data(response)
                .build());
    }
}

