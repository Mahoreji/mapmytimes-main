package in.mapmytour.auth.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for extracting client IP addresses from HTTP requests.
 * Handles Cloudflare, proxies, and direct connections.
 */
@Slf4j
public class IpAddressUtils {

    /**
     * Extract the real client IP address from request.
     * Priority: CF-Connecting-IP (Cloudflare) > X-Forwarded-For > X-Real-IP > RemoteAddr
     * 
     * @param request HTTP servlet request
     * @return Client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        // Priority 1: CF-Connecting-IP (Cloudflare header with real client IP)
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.trim().isEmpty()) {
            String ip = cfConnectingIp.trim();
            log.debug("Extracted client IP from CF-Connecting-IP: {}", ip);
            return ip;
        }

        // Priority 2: X-Forwarded-For (standard proxy header)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // Take the first IP (original client) from the chain
            String ip = xForwardedFor.split(",")[0].trim();
            log.debug("Extracted client IP from X-Forwarded-For: {}", ip);
            return ip;
        }

        // Priority 3: X-Real-IP (nginx/other proxies)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            String ip = xRealIp.trim();
            log.debug("Extracted client IP from X-Real-IP: {}", ip);
            return ip;
        }

        // Priority 4: RemoteAddr (direct connection, no proxy)
        String remoteAddr = request.getRemoteAddr();
        log.debug("Extracted client IP from RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }
}

