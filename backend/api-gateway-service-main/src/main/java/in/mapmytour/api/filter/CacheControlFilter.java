package in.mapmytour.api.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Cache Control Filter
 * Adds Cache-Control headers to responses to prevent unnecessary page reloads
 * GET requests are cached, while POST/PUT/DELETE requests are not cached
 */
@Component
public class CacheControlFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CacheControlFilter.class);
    
    // Cache duration in seconds
    private static final int CACHE_MAX_AGE = 3600; // 1 hour for GET requests
    private static final int STATIC_CACHE_MAX_AGE = 86400; // 24 hours for static resources
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        
        // Only add cache headers for successful GET requests
        if (method == HttpMethod.GET) {
            // Check if this is a static resource
            if (isStaticResource(path)) {
                response.getHeaders().add("Cache-Control", 
                    "public, max-age=" + STATIC_CACHE_MAX_AGE + ", immutable");
                response.getHeaders().add("Expires", 
                    String.valueOf(System.currentTimeMillis() / 1000 + STATIC_CACHE_MAX_AGE));
            } else {
                // For dynamic API GET requests, disable caching to ensure real-time data consistency
                response.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
                response.getHeaders().add("Pragma", "no-cache");
                response.getHeaders().add("Expires", "0");
            }
        } else {
            // For POST, PUT, DELETE, PATCH - no caching
            response.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
            response.getHeaders().add("Pragma", "no-cache");
            response.getHeaders().add("Expires", "0");
        }
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Only apply cache headers if response is successful
            if (response.getStatusCode() != null && 
                response.getStatusCode().is2xxSuccessful() && 
                method == HttpMethod.GET) {
                // Headers already set above, just log
                log.debug("Cache-Control headers added for GET request: {}", path);
            }
        }));
    }
    
    private boolean isStaticResource(String path) {
        return path.endsWith(".js") || 
               path.endsWith(".css") || 
               path.endsWith(".png") || 
               path.endsWith(".jpg") || 
               path.endsWith(".jpeg") || 
               path.endsWith(".gif") || 
               path.endsWith(".svg") || 
               path.endsWith(".ico") || 
               path.endsWith(".woff") || 
               path.endsWith(".woff2") || 
               path.endsWith(".ttf") || 
               path.endsWith(".eot");
    }
    
    private String generateETag(String path) {
        // Simple ETag generation based on path
        // In production, you might want to use content hash
        return "\"" + Integer.toHexString(path.hashCode()) + "\"";
    }
    
    @Override
    public int getOrder() {
        return -50; // Execute after CorrelationIdFilter but before other filters
    }
}

