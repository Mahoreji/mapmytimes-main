package in.mapmytour.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Tracing Configuration for Zipkin
 * Only enables tracing if ZIPKIN_ENDPOINT environment variable is set
 */
@Configuration
public class TracingConfig {

    private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

    @PostConstruct
    public void configureTracing() {
        String zipkinEndpoint = System.getenv("ZIPKIN_ENDPOINT");
        
        if (zipkinEndpoint != null && !zipkinEndpoint.trim().isEmpty()) {
            log.info("✅ Zipkin tracing enabled - endpoint: {}", zipkinEndpoint);
            // Enable tracing
            System.setProperty("management.tracing.enabled", "true");
            System.setProperty("management.zipkin.tracing.endpoint", zipkinEndpoint);
        } else {
            log.info("ℹ️  Zipkin tracing disabled - ZIPKIN_ENDPOINT not set (no connection attempts to localhost:9411)");
            // Ensure tracing is disabled to avoid connection refused errors
            System.setProperty("management.tracing.enabled", "false");
        }
    }
}

