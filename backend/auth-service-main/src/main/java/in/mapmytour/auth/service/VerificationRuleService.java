package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.verification.VerificationRuleDto;

import java.util.List;

public interface VerificationRuleService {
    VerificationRuleDto createRule(VerificationRuleDto request);

    VerificationRuleDto updateRule(String id, VerificationRuleDto request);

    MessageResponse deleteRule(String id);

    List<VerificationRuleDto> getAllRules();

    List<VerificationRuleDto> getRulesByRoleType(String roleType);

    VerificationRuleDto getRuleById(String id);
}
