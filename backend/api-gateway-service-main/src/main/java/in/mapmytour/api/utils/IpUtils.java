package in.mapmytour.api.utils;

import org.springframework.http.server.reactive.ServerHttpRequest;
import java.util.List;

/**
 * Utility class for IP address resolution and security.
 */
public class IpUtils {

    /**
     * Resolves the client IP address from the request, taking into account
     * proxy headers like X-Forwarded-For and X-Real-IP.
     * 
     * @param request The incoming HTTP request
     * @return The resolved client IP address or "unknown"
     */
    public static String resolveClientIp(ServerHttpRequest request) {
        // 1. Check X-Forwarded-For (standard for multi-hop proxies)
        List<String> xff = request.getHeaders().get("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            String firstIp = xff.get(0).split(",")[0].trim();
            if (!firstIp.isBlank()) {
                return firstIp;
            }
        }

        // 2. Check X-Real-IP (common for Nginx/single-hop proxies)
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        // 3. Fallback to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * Checks if an IP is a localhost address.
     */
    public static boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equals(ip);
    }
}
