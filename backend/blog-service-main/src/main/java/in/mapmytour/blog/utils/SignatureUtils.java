package in.mapmytour.blog.utils;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for verifying HMAC signatures from the API Gateway.
 * Pattern matched from hotel-services.
 */
@Slf4j
public class SignatureUtils {
    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Verifies the signature of the provided headers using the given secret.
     */
    public static boolean verifySignature(Map<String, String> headers, String signature, String secret) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }

        try {
            String expectedSignature = generateSignature(headers, secret);
            boolean isValid = expectedSignature.equals(signature);
            
            if (!isValid) {
                log.warn("Invalid gateway signature detected. Received: {}", signature);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying gateway signature: {}", e.getMessage());
            return false;
        }
    }

    public static String generateSignature(Map<String, String> headers, String secret) throws Exception {
        TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);
        
        StringBuilder sb = new StringBuilder();
        sortedHeaders.forEach((key, value) -> {
            if (value != null) {
                sb.append(key).append("=").append(value).append(";");
            }
        });

        String data = sb.toString();
        
        Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        sha256Hmac.init(secretKey);

        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
