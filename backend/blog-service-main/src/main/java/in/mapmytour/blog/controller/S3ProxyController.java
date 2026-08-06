package in.mapmytour.blog.controller;

import in.mapmytour.blog.helper.S3Helper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * S3 reverse proxy controller — streams media files (images/videos) from the
 * project S3 bucket through the Blog Service REST origin.
 *
 * <p>Why this exists: S3 buckets by default do not return CORS headers for every
 * origin, and even with a permissive CORS config some CDNs/browsers block
 * direct XHR/fetch of images from third-party hosts when they are referenced
 * inside a Flutter Web / SPA canvas. This controller exposes the same objects
 * on the same origin ({@code /s3/<object-key>}) so the browser treats the
 * image as same-origin and no CORS preflight is required.</p>
 *
 * <p>Only safe, idempotent GET requests are accepted; uploads/deletes remain
 * behind authenticated admin endpoints.</p>
 */
@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
@Slf4j
public class S3ProxyController {

    private final S3Helper s3Helper;

    /**
     * Extensions considered safe to proxy publicly. Everything else returns 403.
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "ico", "bmp",
            "mp4", "webm", "mov", "mp3", "wav", "ogg",
            "pdf", "txt", "md"
    );

    /**
     * Public alias for {@code GET /s3/**} — remaining path is the S3 object key.
     */
    @GetMapping("/**")
    public void proxyGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String pattern = (String) request.getAttribute(
                org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );
        final String remaining = new AntPathMatcher()
                .extractPathWithinPattern(pattern == null ? "/s3/**" : pattern, request.getRequestURI())
                .replaceAll("^/+", "");

        if (remaining.isBlank()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        final String key = remaining;
        final String ext = extensionOf(key).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            log.warn("Rejecting S3 proxy request for extension='{}' key='{}'", ext, key);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        final S3Client s3 = s3Helper.getS3Client();
        final String bucket = s3Helper.getBucketName();

        try {
            final GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            try (InputStream in = s3.getObject(req)) {
                final String contentType = guessContentType(ext);
                if (contentType != null) response.setContentType(contentType);
                response.setHeader("Cache-Control", "public, max-age=604800, immutable");
                StreamUtils.copy(in, response.getOutputStream());
                response.setStatus(HttpStatus.OK.value());
                response.flushBuffer();
            }
        } catch (NoSuchKeyException nsk) {
            log.debug("S3 proxy 404 — bucket={} key={}", bucket, key);
            response.setStatus(HttpStatus.NOT_FOUND.value());
        } catch (S3Exception s3e) {
            log.warn("S3 proxy S3Exception code={} message={}", s3e.awsErrorDetails().errorCode(), s3e.getMessage());
            response.setStatus(HttpStatus.BAD_GATEWAY.value());
        } catch (RuntimeException rte) {
            log.error("S3 proxy failed for key={}", key, rte);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static String extensionOf(String key) {
        final int dot = key.lastIndexOf('.');
        if (dot < 0 || dot == key.length() - 1) return "";
        return key.substring(dot + 1);
    }

    private static String guessContentType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            case "bmp" -> "image/bmp";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mov" -> "video/quicktime";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "pdf" -> "application/pdf";
            case "txt", "md" -> "text/plain;charset=UTF-8";
            default -> null;
        };
    }
}
