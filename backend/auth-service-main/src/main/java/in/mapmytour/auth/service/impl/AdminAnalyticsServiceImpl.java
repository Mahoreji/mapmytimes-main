package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.analytics.AgentAnalyticsResponse;
import in.mapmytour.auth.dto.analytics.SupplierAnalyticsResponse;
import in.mapmytour.auth.repository.AgentRepository;
import in.mapmytour.auth.repository.SupplierRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.repository.VerificationRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import in.mapmytour.auth.entity.Agent;
import in.mapmytour.auth.entity.Supplier;
import in.mapmytour.auth.entity.VerificationRequest;
import in.mapmytour.auth.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

        private final AgentRepository agentRepository;
        private final SupplierRepository supplierRepository;
        private final UserRepository userRepository;
        private final VerificationRequestRepository verificationRequestRepository;

        @Override
        public AgentAnalyticsResponse getAgentAnalytics() {
                long total = agentRepository.count();
                long active = agentRepository.countByUserIsActive(true);
                long inactive = agentRepository.countByUserIsActive(false);
                long verified = agentRepository.countByUserIsVerified(true);
                long unverified = agentRepository.countByUserIsVerified(false);

                // Optimized counts using repository methods
                long pending = verificationRequestRepository.countByVerificationTypeAndStatus("AGENT",
                                VerificationRequest.VerificationStatus.PENDING);
                long autoVerified = verificationRequestRepository.countByVerificationTypeAndStatusAndReviewedByIsNull(
                                "AGENT", VerificationRequest.VerificationStatus.APPROVED);
                long manuallyVerified = verificationRequestRepository
                                .countByVerificationTypeAndStatusAndReviewedByIsNotNull("AGENT",
                                                VerificationRequest.VerificationStatus.APPROVED);

                return AgentAnalyticsResponse.builder()
                                .totalAgents(total)
                                .activeAgents(active)
                                .inactiveAgents(inactive)
                                .verifiedAgents(verified)
                                .unverifiedAgents(unverified)
                                .pendingVerificationAgents(pending)
                                .autoVerifiedAgents(autoVerified)
                                .manuallyVerifiedAgents(manuallyVerified)
                                .agentsWithGstin(agentRepository.countAgentsWithGstin())
                                .agentsWithPan(agentRepository.countAgentsWithPan())
                                .agentsByCity(toMap(agentRepository.countByCity()))
                                .agentsByState(toMap(agentRepository.countByState()))
                                .agentsByCountry(toMap(agentRepository.countByCountry()))
                                .agentsByBusinessType(toMap(agentRepository.countByBusinessType()))
                                .agentsByBusinessCategory(toMap(agentRepository.countByBusinessCategory()))
                                .registrationTrend(toMap(agentRepository.countRegistrationsByMonth()))
                                .build();
        }

        @Override
        public Page<Agent> getAllAgents(Boolean active, Boolean verified, String city, String state, String search,
                        Pageable pageable) {
                String cityPattern = city != null ? "%" + city.toLowerCase() + "%" : null;
                String statePattern = state != null ? "%" + state.toLowerCase() + "%" : null;
                String searchPattern = search != null ? "%" + search.toLowerCase() + "%" : null;
                return agentRepository.findAllWithFilters(active, verified, cityPattern, statePattern, searchPattern,
                                pageable);
        }

        @Override
        public SupplierAnalyticsResponse getSupplierAnalytics() {
                long total = supplierRepository.count();
                long active = supplierRepository.countByUserIsActive(true);
                long inactive = supplierRepository.countByUserIsActive(false);
                long verified = supplierRepository.countByUserIsVerified(true);
                long unverified = supplierRepository.countByUserIsVerified(false);

                long pending = verificationRequestRepository.countByVerificationTypeAndStatus("SUPPLIER",
                                VerificationRequest.VerificationStatus.PENDING);
                long autoVerified = verificationRequestRepository.countByVerificationTypeAndStatusAndReviewedByIsNull(
                                "SUPPLIER", VerificationRequest.VerificationStatus.APPROVED);
                long manuallyVerified = verificationRequestRepository
                                .countByVerificationTypeAndStatusAndReviewedByIsNotNull("SUPPLIER",
                                                VerificationRequest.VerificationStatus.APPROVED);

                return SupplierAnalyticsResponse.builder()
                                .totalSuppliers(total)
                                .activeSuppliers(active)
                                .inactiveSuppliers(inactive)
                                .verifiedSuppliers(verified)
                                .unverifiedSuppliers(unverified)
                                .pendingVerificationSuppliers(pending)
                                .autoVerifiedSuppliers(autoVerified)
                                .manuallyVerifiedSuppliers(manuallyVerified)
                                .suppliersWithGstin(supplierRepository.countSuppliersWithGstin())
                                .suppliersWithPan(supplierRepository.countSuppliersWithPan())
                                .suppliersByCity(toMap(supplierRepository.countByCity()))
                                .suppliersByState(toMap(supplierRepository.countByState()))
                                .suppliersByCountry(toMap(supplierRepository.countByCountry()))
                                .suppliersByType(toMap(supplierRepository.countBySupplierType()))
                                .suppliersByBusinessCategory(toMap(supplierRepository.countByBusinessCategory()))
                                .registrationTrend(toMap(supplierRepository.countRegistrationsByMonth()))
                                .build();
        }

        @Override
        public Page<Supplier> getAllSuppliers(Boolean active, Boolean verified, String city, String supplierType,
                        String search, Pageable pageable) {
                String cityPattern = city != null ? "%" + city.toLowerCase() + "%" : null;
                String typePattern = supplierType != null ? "%" + supplierType.toLowerCase() + "%" : null;
                String searchPattern = search != null ? "%" + search.toLowerCase() + "%" : null;
                return supplierRepository.findAllWithFilters(active, verified, cityPattern, typePattern, searchPattern,
                                pageable);
        }

        /**
         * Converts JPQL Object[] rows (key, count) to a LinkedHashMap preserving order.
         */
        private Map<String, Long> toMap(List<Object[]> rows) {
                Map<String, Long> result = new LinkedHashMap<>();
                for (Object[] row : rows) {
                        String key = row[0] != null ? row[0].toString() : "Unknown";
                        Long count = ((Number) row[1]).longValue();
                        result.put(key, count);
                }
                return result;
        }

        @Override
        public in.mapmytour.auth.dto.analytics.AdminDashboardResponse getDashboardOverview() {
                long totalUsers = userRepository.count();
                long totalAgents = agentRepository.count();
                long totalSuppliers = supplierRepository.count();
                long activeUsers = userRepository.countByIsActive(true);
                long pendingVerifications = verificationRequestRepository
                                .countByStatus(VerificationRequest.VerificationStatus.PENDING);

                // Global Registration Trend (Based on all users)
                Map<String, Long> combinedTrend = toMap(userRepository.countRegistrationsByMonth());

                // Verification Status Distribution
                Map<String, Long> statusDist = new LinkedHashMap<>();
                statusDist.put("PENDING", pendingVerifications);
                statusDist.put("APPROVED", verificationRequestRepository
                                .countByStatus(VerificationRequest.VerificationStatus.APPROVED));
                statusDist.put("REJECTED", verificationRequestRepository
                                .countByStatus(VerificationRequest.VerificationStatus.REJECTED));

                return in.mapmytour.auth.dto.analytics.AdminDashboardResponse.builder()
                                .totalUsers(totalUsers)
                                .totalAgents(totalAgents)
                                .totalSuppliers(totalSuppliers)
                                .pendingVerifications(pendingVerifications)
                                .activeUsers(activeUsers)
                                .inactiveUsers(totalUsers - activeUsers)
                                .userRegistrationTrend(combinedTrend)
                                .verificationStatusDistribution(statusDist)
                                .build();
        }

        @Override
        public in.mapmytour.auth.dto.analytics.AgentDetailResponse getAgentDetail(String agentId) {
                Agent agent = agentRepository.findById(agentId)
                                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

                // Force initialization of lazy-loaded User relationship
                if (agent.getUser() != null) {
                        agent.getUser().getFirstName(); // Triggers initialization
                }

                List<VerificationRequest> history = verificationRequestRepository
                                .findByUserOrderByCreatedAtDesc(agent.getUser());

                return in.mapmytour.auth.dto.analytics.AgentDetailResponse.builder()
                                .agent(agent)
                                .verificationHistory(history)
                                .build();
        }

        @Override
        public in.mapmytour.auth.dto.analytics.SupplierDetailResponse getSupplierDetail(String supplierId) {
                Supplier supplier = supplierRepository.findById(supplierId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Supplier not found with ID: " + supplierId));

                // Force initialization of lazy-loaded User relationship
                if (supplier.getUser() != null) {
                        supplier.getUser().getFirstName(); // Triggers initialization
                }

                List<VerificationRequest> history = verificationRequestRepository
                                .findByUserOrderByCreatedAtDesc(supplier.getUser());

                return in.mapmytour.auth.dto.analytics.SupplierDetailResponse.builder()
                                .supplier(supplier)
                                .verificationHistory(history)
                                .build();
        }
}
