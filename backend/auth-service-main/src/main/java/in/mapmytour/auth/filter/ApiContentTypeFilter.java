package in.mapmytour.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiContentTypeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path != null && path.startsWith("/api/") && !path.contains("/ws")) {
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("Accept".equalsIgnoreCase(name)) {
                        String accept = super.getHeader(name);
                        if (accept != null && (accept.contains("javascript") || accept.contains("text/javascript"))) {
                            return "application/json";
                        }
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("Accept".equalsIgnoreCase(name)) {
                        String accept = super.getHeader(name);
                        if (accept != null && (accept.contains("javascript") || accept.contains("text/javascript"))) {
                            return Collections.enumeration(List.of("application/json"));
                        }
                    }
                    return super.getHeaders(name);
                }
            };

            HttpServletResponse wrappedResponse = new HttpServletResponseWrapper(response) {
                @Override
                public void setContentType(String type) {
                    if (type != null && (type.contains("javascript") || type.contains("text/javascript"))) {
                        super.setContentType("application/json;charset=UTF-8");
                    } else {
                        super.setContentType(type);
                    }
                }

                @Override
                public void setHeader(String name, String value) {
                    if ("Content-Type".equalsIgnoreCase(name)) {
                        if (value != null && (value.contains("javascript") || value.contains("text/javascript"))) {
                            super.setHeader(name, "application/json;charset=UTF-8");
                            return;
                        }
                    }
                    super.setHeader(name, value);
                }

                @Override
                public void addHeader(String name, String value) {
                    if ("Content-Type".equalsIgnoreCase(name)) {
                        if (value != null && (value.contains("javascript") || value.contains("text/javascript"))) {
                            super.addHeader(name, "application/json;charset=UTF-8");
                            return;
                        }
                    }
                    super.addHeader(name, value);
                }
            };

            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
