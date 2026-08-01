package in.mapmytour.api.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.TreeMap;
import java.util.Map;

/**
 * Utility class for generating HMAC signatures for inter-service communication.
 * Ensures requests to downstream services are authentic and originate from the
 * API Gateway.
 */
@Component
@Slf4j
public class GatewaySignatureUtil {

    private final String secret;

    public GatewaySignatureUtil(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    /**
     * Generates an HMAC-SHA256 signature for the given headers.
     * 
     * @param headers Map of headers to sign (e.g., X-User-Id, X-Gateway-Timestamp)
     * @return Base64 encoded signature
     */
    public String generateSignature(Map<String, String> headers) {
        try {
            // Sort keys to ensure consistent signature generation
            TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);

            StringBuilder sb = new StringBuilder();
            sortedHeaders.forEach((key, value) -> {
                if (value != null) {
                    sb.append(key).append("=").append(value).append(";");
                }
            });

            String data = sb.toString();
            log.debug("Generating signature for data: {}", data);

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error generating gateway signature: {}", e.getMessage(), e);
            throw new RuntimeException("Could not generate gateway signature", e);
        }
    }
}
