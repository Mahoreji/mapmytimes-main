package in.mapmytour.api.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * SecurityHeadersFilter — Global filter that enforces enterprise-grade HTTP
 * security headers on every outbound response, overriding anything that a
 * backend microservice may have set.
 *
 * Implements OWASP Secure Headers Project recommendations and
 * aligns with a Zero-Trust perimeter model.
 *
 * Filter order: LOWEST_PRECEDENCE — runs after the route is resolved and the
 * upstream response is received, so its headers win over any backend headers.
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    // ── HSTS ──────────────────────────────────────────────────────────────
    // 2 years, include sub-domains, submit for browser preload lists
    private static final String HSTS_VALUE =
            "max-age=63072000; includeSubDomains; preload";

    // ── Content-Security-Policy ───────────────────────────────────────────
    // Multi-origin comprehensive policy for Cashfree, Google Analytics, AWS S3, and Embark
    private static final String CSP_VALUE =
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://www.google-analytics.com https://apis.google.com https://sdk.cashfree.com https://*.embarktest.com https://*.embark.com https://*.mapmytimes.com; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data: https://*.amazonaws.com https://*.embarktest.com https://*.embark.com https://*.mapmytimes.com; " +
            "connect-src 'self' https://*.mapmytimes.com https://*.cashfree.com https://*.embarktest.com https://*.embark.com; " +
            "frame-ancestors 'self'; " +
            "base-uri 'self'; " +
            "form-action 'self'";

    // ── Permissions-Policy ────────────────────────────────────────────────
    private static final String PERMISSIONS_POLICY_VALUE =
            "camera=(), microphone=(), geolocation=(), payment=(), usb=(), interest-cohort=()";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> injectHeaders(exchange)));
    }

    private void injectHeaders(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();

        // Already committed — nothing we can do
        if (response.isCommitted()) {
            return;
        }

        var headers = response.getHeaders();

        // 1. Force HTTPS for 2 years on all sub-domains (HSTS)
        headers.set("Strict-Transport-Security", HSTS_VALUE);

        // 2. Restrict loading of resources to the same origin
        headers.set("Content-Security-Policy", CSP_VALUE);

        // 3. Prevent clickjacking — no framing allowed
        headers.set("X-Frame-Options", "DENY");

        // 4. Block MIME-type sniffing
        headers.set("X-Content-Type-Options", "nosniff");

        // 5. Safe referrer leakage policy
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");

        // 6. Disable unused browser APIs
        headers.set("Permissions-Policy", PERMISSIONS_POLICY_VALUE);

        // 7. Prevent window.opener hijacking
        headers.set("Cross-Origin-Opener-Policy", "same-origin");

        // 8. Restrict cross-origin resource sharing at the browser level
        headers.set("Cross-Origin-Resource-Policy", "same-origin");

        // 9. Require CORP headers for embedded resources (COEP)
        headers.set("Cross-Origin-Embedder-Policy", "require-corp");

        // 10. Disable DNS prefetching (information leak reduction)
        headers.set("X-DNS-Prefetch-Control", "off");

        // 11. Block Flash / PDF cross-domain policies
        headers.set("X-Permitted-Cross-Domain-Policies", "none");
    }

    @Override
    public int getOrder() {
        // LOWEST_PRECEDENCE so this runs after upstream response is received,
        // ensuring our headers override any backend service headers
        return Ordered.LOWEST_PRECEDENCE;
    }
}
