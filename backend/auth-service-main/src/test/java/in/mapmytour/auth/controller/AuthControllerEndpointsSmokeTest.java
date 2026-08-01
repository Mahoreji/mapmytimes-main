package in.mapmytour.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Smoke test that ensures every endpoint defined in {@link AuthController}
 * is reachable (i.e. does not return HTTP 404).
 *
 * This does NOT validate business logic; it only guarantees that
 * request mappings are correctly registered and wired.
 */
@WebMvcTest(controllers = AuthController.class)
class AuthControllerEndpointsSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockBean
    private in.mapmytour.auth.service.AuthService authService;

    @MockBean
    private in.mapmytour.auth.service.UserContextService userContextService;

    @MockBean
    private in.mapmytour.auth.utils.SignatureUtils signatureUtils;

    @Test
    void allAuthControllerEndpointsAreReachable() throws Exception {
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();

            if (!handlerMethod.getBeanType().equals(AuthController.class)) {
                continue;
            }

            RequestMappingInfo info = entry.getKey();
            Set<String> patterns = info.getPathPatternsCondition() != null
                    ? info.getPathPatternsCondition().getPatternValues()
                    : info.getPatternsCondition().getPatterns();

            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods == null || methods.isEmpty()) {
                methods = Set.of(RequestMethod.GET);
            }

            for (String pattern : patterns) {
                String url = pattern.replaceAll("\\{[^/]+}", "test-id");

                for (RequestMethod method : methods) {
                    MockHttpServletRequestBuilder builder = createRequestBuilder(method, url);

                    if (builder == null) {
                        continue;
                    }

                    if (method == RequestMethod.POST || method == RequestMethod.PUT || method == RequestMethod.PATCH) {
                        builder.contentType(MediaType.APPLICATION_JSON)
                                .content("{}");
                    }

                    mockMvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                            .andExpect(result -> {
                                int status = result.getResponse().getStatus();
                                assertNotEquals(404, status,
                                        () -> "Endpoint " + method + " " + url + " returned 404");
                            });
                }
            }
        }
    }

    private MockHttpServletRequestBuilder createRequestBuilder(RequestMethod method, String url) {
        return switch (method) {
            case GET -> get(url);
            case POST -> post(url);
            case PUT -> put(url);
            case PATCH -> patch(url);
            case DELETE -> delete(url);
            default -> null;
        };
    }
}


