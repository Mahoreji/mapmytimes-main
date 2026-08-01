package in.mapmytour.blog.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class TomcatMultipartLimitsConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMultipartPartCountCustomizer(Environment environment) {
        int maxPartCount = environment.getProperty("server.tomcat.max-part-count", Integer.class, 200);
        if (maxPartCount < 1) {
            maxPartCount = 200;
        }
        int configured = maxPartCount;
        return factory -> factory.addConnectorCustomizers(connector -> connector.setMaxPartCount(configured));
    }
}
