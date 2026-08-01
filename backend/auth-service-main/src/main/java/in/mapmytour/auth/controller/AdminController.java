package in.mapmytour.auth.controller;

import in.mapmytour.auth.dto.APIResponse;
import in.mapmytour.auth.dto.analytics.AgentAnalyticsResponse;
import in.mapmytour.auth.dto.analytics.SupplierAnalyticsResponse;
import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.verification.VerificationRuleDto;
import in.mapmytour.auth.entity.Agent;
import in.mapmytour.auth.entity.Supplier;
import in.mapmytour.auth.repository.AgentRepository;
import in.mapmytour.auth.repository.SupplierRepository;
import in.mapmytour.auth.service.AdminAnalyticsService;
import in.mapmytour.auth.service.VerificationRuleService;
import in.mapmytour.auth.service.UserService;
import in.mapmytour.auth.dto.user.AdminVerificationActionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VerificationRuleService verificationRuleService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final AgentRepository agentRepository;
    private final SupplierRepository supplierRepository;
    private final UserService userService;

    // --- Verification Rules Endpoints ---

    @PostMapping("/verification-rules")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<VerificationRuleDto>> createRule(
            @Valid @RequestBody VerificationRuleDto request) {
        VerificationRuleDto created = verificationRuleService.createRule(request);
        APIResponse<VerificationRuleDto> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Verification rule created successfully");
        response.setData(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/verification-rules/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<VerificationRuleDto>> updateRule(
            @PathVariable String id,
            @Valid @RequestBody VerificationRuleDto request) {
        VerificationRuleDto updated = verificationRuleService.updateRule(id, request);
        APIResponse<VerificationRuleDto> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Verification rule updated successfully");
        response.setData(updated);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/verification-rules/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> deleteRule(@PathVariable String id) {
        MessageResponse msg = verificationRuleService.deleteRule(id);
        APIResponse<MessageResponse> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Verification rule deleted successfully");
        response.setData(msg);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verification-rules")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<List<VerificationRuleDto>>> getAllRules(
            @RequestParam(required = false) String roleType) {
        List<VerificationRuleDto> rules = (roleType != null && !roleType.isEmpty())
                ? verificationRuleService.getRulesByRoleType(roleType)
                : verificationRuleService.getAllRules();
        APIResponse<List<VerificationRuleDto>> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Verification rules retrieved successfully");
        response.setData(rules);
        return ResponseEntity.ok(response);
    }

    // --- Analytics Endpoints ---

    @GetMapping("/analytics/agents")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<AgentAnalyticsResponse>> getAgentAnalytics() {
        AgentAnalyticsResponse analytics = adminAnalyticsService.getAgentAnalytics();
        APIResponse<AgentAnalyticsResponse> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Agent analytics retrieved successfully");
        response.setData(analytics);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/suppliers")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<SupplierAnalyticsResponse>> getSupplierAnalytics() {
        SupplierAnalyticsResponse analytics = adminAnalyticsService.getSupplierAnalytics();
        APIResponse<SupplierAnalyticsResponse> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Supplier analytics retrieved successfully");
        response.setData(analytics);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<in.mapmytour.auth.dto.analytics.AdminDashboardResponse>> getDashboardOverview() {
        in.mapmytour.auth.dto.analytics.AdminDashboardResponse dashboard = adminAnalyticsService.getDashboardOverview();
        APIResponse<in.mapmytour.auth.dto.analytics.AdminDashboardResponse> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Admin dashboard overview retrieved successfully");
        response.setData(dashboard);
        return ResponseEntity.ok(response);
    }

    // --- User Management List Endpoints ---

    @GetMapping("/agents")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<Page<Agent>>> getAllAgents(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Agent> agents = adminAnalyticsService.getAllAgents(active, verified, city, state, search, pageable);

        APIResponse<Page<Agent>> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Agents retrieved successfully");
        response.setData(agents);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agents/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<in.mapmytour.auth.dto.analytics.AgentDetailResponse>> getAgentDetail(
            @PathVariable String id) {
        in.mapmytour.auth.dto.analytics.AgentDetailResponse detail = adminAnalyticsService.getAgentDetail(id);
        APIResponse<in.mapmytour.auth.dto.analytics.AgentDetailResponse> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Agent details retrieved successfully");
        response.setData(detail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<Page<Supplier>>> getAllSuppliers(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String supplierType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Supplier> suppliers = adminAnalyticsService.getAllSuppliers(active, verified, city, supplierType, search,
                pageable);

        APIResponse<Page<Supplier>> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Suppliers retrieved successfully");
        response.setData(suppliers);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<in.mapmytour.auth.dto.analytics.SupplierDetailResponse>> getSupplierDetail(
            @PathVariable String id) {
        in.mapmytour.auth.dto.analytics.SupplierDetailResponse detail = adminAnalyticsService.getSupplierDetail(id);
        APIResponse<in.mapmytour.auth.dto.analytics.SupplierDetailResponse> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Supplier details retrieved successfully");
        response.setData(detail);
        return ResponseEntity.ok(response);
    }

    // --- Verification Actions ---

    @PostMapping("/verify-user/{requestId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<APIResponse<MessageResponse>> handleVerification(
            @PathVariable String requestId,
            @Valid @RequestBody AdminVerificationActionRequest request,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        MessageResponse msg;

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            msg = userService.approveVerificationRequest(requestId, adminEmail, request.getAdminNotes());
        } else {
            msg = userService.rejectVerificationRequest(requestId, adminEmail, request.getAdminNotes());
        }

        APIResponse<MessageResponse> response = new APIResponse<>();
        response.setSuccess(true);
        response.setMessage("Verification request " + request.getAction().toLowerCase() + "d successfully");
        response.setData(msg);
        return ResponseEntity.ok(response);
    }
}
