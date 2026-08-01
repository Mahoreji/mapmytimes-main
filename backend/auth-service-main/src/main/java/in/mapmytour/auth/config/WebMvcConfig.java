package in.mapmytour.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        boolean hasJackson = converters.stream()
                .anyMatch(c -> c instanceof MappingJackson2HttpMessageConverter);
        if (!hasJackson) {
            converters.add(new MappingJackson2HttpMessageConverter());
        }
    }
}
