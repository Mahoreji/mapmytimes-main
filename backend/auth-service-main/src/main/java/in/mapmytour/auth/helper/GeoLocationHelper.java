package in.mapmytour.auth.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoLocationHelper {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Cache to avoid too many API calls for the same IP
    private final ConcurrentHashMap<String, String> locationCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = TimeUnit.HOURS.toMillis(24); // Cache for 24 hours

    /**
     * Get location string from IP address
     * Format: "City, State, Country" or "Country" if city/state not available
     */
    public String getLocationFromIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("127.0.0.1")) {
            return "Local";
        }

        // Check cache first
        String cachedLocation = locationCache.get(ipAddress);
        if (cachedLocation != null) {
            return cachedLocation;
        }

        try {
            // Using ip-api.com free service (no API key required, rate limit: 45 requests/minute)
            String url = "http://ip-api.com/json/" + ipAddress + "?fields=status,message,country,regionName,city,query";
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("status") && "success".equals(jsonNode.get("status").asText())) {
                String country = jsonNode.has("country") ? jsonNode.get("country").asText() : "";
                String region = jsonNode.has("regionName") ? jsonNode.get("regionName").asText() : "";
                String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "";

                String location = buildLocationString(city, region, country);
                
                // Cache the result
                locationCache.put(ipAddress, location);
                
                return location;
            } else {
                log.debug("Failed to get location for IP {}: {}", ipAddress, jsonNode.has("message") ? jsonNode.get("message").asText() : "Unknown error");
                return "Unknown";
            }
        } catch (Exception e) {
            log.warn("Error getting location for IP {}: {}", ipAddress, e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Build location string from city, region, and country
     */
    private String buildLocationString(String city, String region, String country) {
        if (city != null && !city.isEmpty() && region != null && !region.isEmpty()) {
            return city + ", " + region + ", " + country;
        } else if (city != null && !city.isEmpty()) {
            return city + ", " + country;
        } else if (region != null && !region.isEmpty()) {
            return region + ", " + country;
        } else if (country != null && !country.isEmpty()) {
            return country;
        }
        return "Unknown";
    }

    /**
     * Clear cache (useful for testing or if you want to refresh locations)
     */
    public void clearCache() {
        locationCache.clear();
    }
}

