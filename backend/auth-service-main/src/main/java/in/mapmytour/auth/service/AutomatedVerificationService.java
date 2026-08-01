package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.auth.RegisterAgentRequest;
import in.mapmytour.auth.dto.auth.RegisterSupplierRequest;
import in.mapmytour.auth.entity.VerificationRule;

import java.util.List;

public interface AutomatedVerificationService {

    /**
     * Executes automated verification rules on the provided agent registration
     * request.
     * 
     * @param request        the agent registration payload
     * @param mandatoryRules the mandatory rules for the AGENT role
     * @return true if ALL automated verification rules pass, false otherwise (e.g.
     *         if one fails or needs manual review)
     */
    boolean verifyAgentAutomatically(RegisterAgentRequest request, List<VerificationRule> mandatoryRules);

    /**
     * Executes automated verification rules on the provided supplier registration
     * request.
     * 
     * @param request        the supplier registration payload
     * @param mandatoryRules the mandatory rules for the SUPPLIER role
     * @return true if ALL automated verification rules pass, false otherwise (e.g.
     *         if one fails or needs manual review)
     */
    boolean verifySupplierAutomatically(RegisterSupplierRequest request, List<VerificationRule> mandatoryRules);

}
