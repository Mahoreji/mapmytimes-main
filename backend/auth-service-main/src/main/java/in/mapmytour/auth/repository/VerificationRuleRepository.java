package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.VerificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRuleRepository extends JpaRepository<VerificationRule, String> {
    List<VerificationRule> findByRoleType(String roleType);

    Optional<VerificationRule> findByRoleTypeAndFieldName(String roleType, String fieldName);
}
