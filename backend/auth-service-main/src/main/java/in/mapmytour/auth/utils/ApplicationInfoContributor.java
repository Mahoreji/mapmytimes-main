package in.mapmytour.auth.utils;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApplicationInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("name", "MapMyTour");
        appInfo.put("description", "Tour Planning and Management System");
        appInfo.put("version", "1.0.0");
        appInfo.put("startup-time", LocalDateTime.now().toString());

        Map<String, Object> contact = new HashMap<>();
        contact.put("email", "support@mapmytour.in");
        contact.put("website", "https://mapmytour.in");

        appInfo.put("contact", contact);

        builder.withDetail("application", appInfo);
    }
}