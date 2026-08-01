// Add generic octet stream support for multiple DTOs
package in.mapmytour.blog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.IOException;

/**
 * Custom resolver to auto-deserialize @OctetJson annotated parameters
 */
@Component
@RequiredArgsConstructor
public class OctetJsonArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(OctetJson.class);
    }

    @Override
    @Nullable
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) throws Exception {

        try {
            HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
            if (servletRequest == null) {
                throw new HttpMessageNotReadableException("Failed to get HttpServletRequest");
            }
            return objectMapper.readValue(servletRequest.getInputStream(),
                    objectMapper.constructType(parameter.getGenericParameterType()));
        } catch (IOException e) {
            throw new HttpMessageNotReadableException("Failed to read octet-stream body", e);
        }
    }
}
