package in.mapmytour.auth.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility for verifying HMAC-SHA256 signatures from the API Gateway.
 */
@Component
@Slf4j
public class SignatureUtils {

    private final String secret;

    public SignatureUtils(@Value("${GATEWAY_JWT_SECRET}") String secret) {
        this.secret = secret;
    }

    /**
     * Verifies if the signature matches the provided headers.
     */
    public boolean verifySignature(Map<String, String> headers, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isEmpty()) {
            return false;
        }

        try {
            String expectedSignature = generateSignature(headers);
            return receivedSignature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Error verifying signature: {}", e.getMessage());
            return false;
        }
    }

    public String generateSignature(Map<String, String> headers) {
        try {
            TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);
            StringBuilder sb = new StringBuilder();
            sortedHeaders.forEach((key, value) -> {
                if (value != null) {
                    sb.append(key).append("=").append(value).append(";");
                }
            });

            String data = sb.toString();
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error generating signature for verification: {}", e.getMessage());
            throw new RuntimeException("Could not generate signature", e);
        }
    }
}
