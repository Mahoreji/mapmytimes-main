package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.auth.RegisterAgentRequest;
import in.mapmytour.auth.dto.auth.RegisterSupplierRequest;
import in.mapmytour.auth.entity.VerificationRule;
import in.mapmytour.auth.service.AutomatedVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomatedVerificationServiceImpl implements AutomatedVerificationService {

    @Override
    public boolean verifyAgentAutomatically(RegisterAgentRequest request, List<VerificationRule> mandatoryRules) {
        if (mandatoryRules == null || mandatoryRules.isEmpty()) {
            // If there are no mandatory rules, we cannot safely auto-approve. Return false
            // to gracefully fall back to Admin review.
            return false;
        }

        for (VerificationRule rule : mandatoryRules) {
            if (!rule.getIsAutomated()) {
                // If a mandatory rule explicitly requires manual review (not automated),
                // auto-approval fails.
                return false;
            }

            // Execute the automated check based on the automation type
            boolean passed = executeAutomationRule(rule, getAgentFieldValue(request, rule.getFieldName()));
            if (!passed) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean verifySupplierAutomatically(RegisterSupplierRequest request, List<VerificationRule> mandatoryRules) {
        if (mandatoryRules == null || mandatoryRules.isEmpty()) {
            return false; // Fallback to manual review
        }

        for (VerificationRule rule : mandatoryRules) {
            if (!rule.getIsAutomated()) {
                return false; // Manual review required for at least one mandatory rule
            }

            boolean passed = executeAutomationRule(rule, getSupplierFieldValue(request, rule.getFieldName()));
            if (!passed) {
                return false;
            }
        }

        return true;
    }

    private boolean executeAutomationRule(VerificationRule rule, String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        switch (rule.getAutomationType()) {
            case REGEX:
                // Fallback basic validation
                if ("gstin".equalsIgnoreCase(rule.getFieldName())) {
                    return value.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
                }
                if ("pan".equalsIgnoreCase(rule.getFieldName())) {
                    return value.matches("[A-Z]{5}[0-9]{4}[A-Z]{1}");
                }
                return true; // Unknown field name regex, bypass or implement further parsing
            case EXTERNAL_API:
                // Mock External API Call
                log.info("Mocking External API call to verify {} : {}", rule.getFieldName(), value);

                if ("gstin".equalsIgnoreCase(rule.getFieldName()) && value.startsWith("00")) {
                    return false; // Mock failure condition
                }
                return true; // Mock success
            case DOCUMENT_AI:
                log.warn("DOCUMENT_AI automation type is not yet implemented natively. Falling back to false.");
                return false;
            default:
                return false;
        }
    }

    private String getAgentFieldValue(RegisterAgentRequest request, String fieldName) {
        if ("gstin".equalsIgnoreCase(fieldName))
            return request.getGstin();
        if ("pan".equalsIgnoreCase(fieldName))
            return request.getPan();
        return null;
    }

    private String getSupplierFieldValue(RegisterSupplierRequest request, String fieldName) {
        if ("gstin".equalsIgnoreCase(fieldName))
            return request.getGstin();
        if ("pan".equalsIgnoreCase(fieldName))
            return request.getPan();
        return null;
    }
}
