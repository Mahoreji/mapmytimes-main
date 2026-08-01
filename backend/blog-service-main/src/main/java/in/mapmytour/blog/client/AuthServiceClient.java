package in.mapmytour.blog.client;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.external.InternalNotificationRequest;
import in.mapmytour.blog.dto.external.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    @Value("${GATEWAY_JWT_SECRET:mapmytour_secret_key_2024}")
    private String gatewaySecret;

    public AuthServiceClient(RestTemplate restTemplate, @Value("${app.auth-service.url}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    /**
     * Fetch user profile from auth-service by user ID.
     * Uses the public profile endpoint.
     */
    public UserProfileResponse getUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        String url = authServiceUrl + "/api/v1/user/profile/" + userId;
        try {
            log.debug("Fetching user profile from: {}", url);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            
            // Prepare headers for signing
            Map<String, String> headersToSign = new HashMap<>();
            headersToSign.put("X-Request-Source", "internal-service");
            headersToSign.put("X-Gateway-Timestamp", timestamp);
            
            try {
                String signature = in.mapmytour.blog.utils.SignatureUtils.generateSignature(headersToSign, gatewaySecret);
                
                headers.set("X-Request-Source", "internal-service");
                headers.set("X-Gateway-Timestamp", timestamp);
                headers.set("X-Gateway-Signature", signature);
                
                log.debug("Using internal-service signature for auth-service call");
            } catch (Exception e) {
                log.error("Failed to generate signature for auth-service call: {}", e.getMessage());
                // Fallback to basic header if signature fails (likely will be blocked by filter)
                headers.set("X-Request-Source", "api-gateway");
            }

            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<APIResponse<UserProfileResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<APIResponse<UserProfileResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            } else {
                log.warn("Failed to fetch user profile for userId: {}. Message: {}", 
                        userId, response.getBody() != null ? response.getBody().getMessage() : "No body");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.info("Profile not available for userId ({}): {}", userId, e.getStatusCode());
                log.debug("Response body: {}", e.getResponseBodyAsString());
            } else {
                log.warn("Error calling auth-service for userId: {}: {}", userId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error calling auth-service for userId: {}: {}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * Fetch multiple user profiles from auth-service by user IDs.
     * Performs iterative fetching internally to avoid adding a batch endpoint to auth-service.
     */
    public Map<String, UserProfileResponse> getUserProfiles(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, UserProfileResponse> profileMap = new HashMap<>();
        // Deduplicate userIds to minimize calls
        java.util.Set<String> uniqueUserIds = new java.util.HashSet<>(userIds);
        
        log.debug("Iteratively fetching user profiles for {} unique users", uniqueUserIds.size());
        
        for (String userId : uniqueUserIds) {
            try {
                UserProfileResponse profile = getUserProfile(userId);
                if (profile != null) {
                    profileMap.put(userId, profile);
                }
            } catch (Exception e) {
                log.error("Failed to fetch individual profile during batch for userId: {}: {}", userId, e.getMessage());
            }
        }
        
        return profileMap;
    }

    /**
     * Send a social notification to auth-service.
     */
    public void sendNotification(String recipientUserId, String senderUserId, String type, String message, String postId) {
        String url = authServiceUrl + "/api/v1/user/internal/notifications";

        InternalNotificationRequest internalRequest = InternalNotificationRequest.builder()
                .recipientUserId(recipientUserId)
                .senderUserId(senderUserId)
                .type(type)
                .message(message)
                .postId(postId)
                .build();

        try {
            log.debug("Sending notification to auth-service: {}", url);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            
            // Prepare headers for signing
            Map<String, String> headersToSign = new HashMap<>();
            headersToSign.put("X-Request-Source", "internal-service");
            headersToSign.put("X-Gateway-Timestamp", timestamp);
            
            try {
                String signature = in.mapmytour.blog.utils.SignatureUtils.generateSignature(headersToSign, gatewaySecret);
                
                headers.set("X-Request-Source", "internal-service");
                headers.set("X-Gateway-Timestamp", timestamp);
                headers.set("X-Gateway-Signature", signature);
            } catch (Exception e) {
                log.error("Failed to sign internal notification request: {}", e.getMessage());
                headers.set("X-Request-Source", "internal-service");
            }

            org.springframework.http.HttpEntity<InternalNotificationRequest> entity = new org.springframework.http.HttpEntity<>(internalRequest, headers);
            restTemplate.postForEntity(url, entity, APIResponse.class);
        } catch (Exception e) {
            log.error("Failed to send notification to auth-service: {}", e.getMessage());
        }
    }
}
