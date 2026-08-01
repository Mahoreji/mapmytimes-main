package in.mapmytour.auth.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentRequest {

    private UUID userId;
    private String agentCode;
    private String companyName;
    private String contactPerson;
    private String email;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String gstin;
    private String pan;
    private AgentStatus status;
    private AgentTier tier;
    private Map<String, Object> bankDetails;
    private Map<String, Object> kycDocuments;
    private Map<String, Object> metadata;

    public enum AgentStatus {
        PENDING, ACTIVE, INACTIVE, SUSPENDED, TERMINATED
    }

    public enum AgentTier {
        BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
    }
}
