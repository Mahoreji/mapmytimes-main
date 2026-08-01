package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.verification.VerificationRuleDto;
import in.mapmytour.auth.entity.VerificationRule;
import in.mapmytour.auth.repository.VerificationRuleRepository;
import in.mapmytour.auth.service.VerificationRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerificationRuleServiceImpl implements VerificationRuleService {

    private final VerificationRuleRepository verificationRuleRepository;

    @Override
    public VerificationRuleDto createRule(VerificationRuleDto request) {
        if (verificationRuleRepository.findByRoleTypeAndFieldName(request.getRoleType(), request.getFieldName())
                .isPresent()) {
            throw new IllegalArgumentException("A rule for this role and field already exists");
        }

        VerificationRule rule = VerificationRule.builder()
                .roleType(request.getRoleType())
                .fieldName(request.getFieldName())
                .isMandatory(request.getIsMandatory())
                .isAutomated(request.getIsAutomated())
                .automationType(request.getAutomationType())
                .description(request.getDescription())
                .build();

        VerificationRule saved = verificationRuleRepository.save(rule);
        return mapToDto(saved);
    }

    @Override
    public VerificationRuleDto updateRule(String id, VerificationRuleDto request) {
        VerificationRule rule = verificationRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        // Check uniqueness if changing field or role
        if (!rule.getRoleType().equals(request.getRoleType()) || !rule.getFieldName().equals(request.getFieldName())) {
            if (verificationRuleRepository.findByRoleTypeAndFieldName(request.getRoleType(), request.getFieldName())
                    .isPresent()) {
                throw new IllegalArgumentException("A rule for this role and field already exists");
            }
        }

        rule.setRoleType(request.getRoleType());
        rule.setFieldName(request.getFieldName());
        rule.setIsMandatory(request.getIsMandatory());
        rule.setIsAutomated(request.getIsAutomated());
        rule.setAutomationType(request.getAutomationType());
        rule.setDescription(request.getDescription());

        VerificationRule updated = verificationRuleRepository.save(rule);
        return mapToDto(updated);
    }

    @Override
    public MessageResponse deleteRule(String id) {
        VerificationRule rule = verificationRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        verificationRuleRepository.delete(rule);

        return MessageResponse.builder()
                .message("Verification rule deleted successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationRuleDto> getAllRules() {
        return verificationRuleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationRuleDto> getRulesByRoleType(String roleType) {
        return verificationRuleRepository.findByRoleType(roleType.toUpperCase()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationRuleDto getRuleById(String id) {
        VerificationRule rule = verificationRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));
        return mapToDto(rule);
    }

    private VerificationRuleDto mapToDto(VerificationRule rule) {
        return VerificationRuleDto.builder()
                .id(rule.getId())
                .roleType(rule.getRoleType())
                .fieldName(rule.getFieldName())
                .isMandatory(rule.getIsMandatory())
                .isAutomated(rule.getIsAutomated())
                .automationType(rule.getAutomationType())
                .description(rule.getDescription())
                .build();
    }
}
