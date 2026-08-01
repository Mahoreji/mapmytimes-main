package in.mapmytour.customer.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration using Resilience4j
 * Provides fault tolerance for external service calls
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * Circuit Breaker Registry
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Retry Registry
     */
    @Bean
    public RetryRegistry retryRegistry() {
        io.github.resilience4j.retry.RetryConfig config = io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class,
                    org.springframework.web.client.ResourceAccessException.class
                )
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * Time Limiter Registry
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        io.github.resilience4j.timelimiter.TimeLimiterConfig config = io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();

        return TimeLimiterRegistry.of(config);
    }
}

