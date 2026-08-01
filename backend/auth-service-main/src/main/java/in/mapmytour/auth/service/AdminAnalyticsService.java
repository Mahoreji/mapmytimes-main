package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.analytics.AgentAnalyticsResponse;
import in.mapmytour.auth.dto.analytics.SupplierAnalyticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AdminAnalyticsService {

    /**
     * Returns aggregated analytics for all Agents.
     */
    AgentAnalyticsResponse getAgentAnalytics();

    Page<in.mapmytour.auth.entity.Agent> getAllAgents(Boolean active, Boolean verified, String city, String state,
            String search, Pageable pageable);

    /**
     * Returns aggregated analytics for all Suppliers.
     */
    SupplierAnalyticsResponse getSupplierAnalytics();

    Page<in.mapmytour.auth.entity.Supplier> getAllSuppliers(Boolean active, Boolean verified, String city,
            String supplierType, String search, Pageable pageable);

    // --- New Unified Dashboard & Detailed Views ---
    in.mapmytour.auth.dto.analytics.AdminDashboardResponse getDashboardOverview();

    in.mapmytour.auth.dto.analytics.AgentDetailResponse getAgentDetail(String agentId);

    in.mapmytour.auth.dto.analytics.SupplierDetailResponse getSupplierDetail(String supplierId);
}
