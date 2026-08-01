package in.mapmytour.auth.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

@Slf4j
public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        addCookie(response, name, value, maxAge, true, true);
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, 
                                  boolean httpOnly, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);
        // Set SameSite attribute using reflection (Jakarta Servlet API doesn't have direct support)
        try {
            // For SameSite=None, Secure must be true
            if (secure) {
                // Use reflection to set SameSite attribute if available
                java.lang.reflect.Method setAttribute = cookie.getClass().getMethod("setAttribute", String.class, String.class);
                setAttribute.invoke(cookie, "SameSite", "None");
            } else {
                java.lang.reflect.Method setAttribute = cookie.getClass().getMethod("setAttribute", String.class, String.class);
                setAttribute.invoke(cookie, "SameSite", "Lax");
            }
        } catch (Exception e) {
            // If reflection fails, log but continue (older servlet containers may not support this)
            log.debug("Could not set SameSite attribute on cookie {}: {}", name, e.getMessage());
        }
        response.addCookie(cookie);
        log.debug("Set cookie: {} with maxAge={}, httpOnly={}, secure={}", name, maxAge, httpOnly, secure);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    public static String serialize(Object object) {
        try {
            byte[] serialized = SerializationUtils.serialize(object);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(serialized.length);
            java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(bos);
            gzip.write(serialized);
            gzip.close();
            byte[] compressed = bos.toByteArray();
            return Base64.getUrlEncoder().encodeToString(compressed);
        } catch (java.io.IOException e) {
            log.error("Failed to compress cookie", e);
            // Fallback to uncompressed
            return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cookie.getValue());
            try {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(decoded);
                java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bis);
                byte[] decompressed = gzip.readAllBytes();
                Object obj = SerializationUtils.deserialize(decompressed);
                return cls.cast(obj);
            } catch (java.io.IOException e) {
                // Fallback for older uncompressed cookies
                Object obj = SerializationUtils.deserialize(decoded);
                return cls.cast(obj);
            }
        } catch (Exception e) {
            log.error("Failed to deserialize cookie", e);
            return null;
        }
    }
}