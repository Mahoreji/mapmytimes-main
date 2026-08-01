package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.blogsettings.CreateSettingRequest;
import in.mapmytour.blog.dto.request.blogsettings.UpdateSettingRequest;
import in.mapmytour.blog.dto.response.blogsettings.BlogSettingsResponse;
import in.mapmytour.blog.dto.response.BlogStatsResponse;
import in.mapmytour.blog.service.BlogSettingsService;
import in.mapmytour.blog.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/blog/settings")
@RequiredArgsConstructor
@Slf4j
public class BlogSettingsController {

    private final BlogSettingsService blogSettingsService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<BlogSettingsResponse>> createSetting(
            @Valid @RequestBody CreateSettingRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<BlogSettingsResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        BlogSettingsResponse response = blogSettingsService.createSetting(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<BlogSettingsResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Setting created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{settingKey}")
    public ResponseEntity<APIResponse<BlogSettingsResponse>> getSetting(@PathVariable String settingKey) {
        BlogSettingsResponse response = blogSettingsService.getSetting(settingKey);

        return ResponseEntity.ok(APIResponse.<BlogSettingsResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Setting retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<BlogSettingsResponse>>> getAllSettings() {
        List<BlogSettingsResponse> response = blogSettingsService.getAllSettings();

        return ResponseEntity.ok(APIResponse.<List<BlogSettingsResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Settings retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/map")
    public ResponseEntity<APIResponse<Map<String, String>>> getSettingsMap() {
        Map<String, String> response = blogSettingsService.getSettingsMap();

        return ResponseEntity.ok(APIResponse.<Map<String, String>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Settings map retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{settingKey}")
    public ResponseEntity<APIResponse<BlogSettingsResponse>> updateSetting(
            @PathVariable String settingKey,
            @Valid @RequestBody UpdateSettingRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<BlogSettingsResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        BlogSettingsResponse response = blogSettingsService.updateSetting(settingKey, request);

        return ResponseEntity.ok(APIResponse.<BlogSettingsResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Setting updated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{settingKey}")
    public ResponseEntity<APIResponse<Void>> deleteSetting(
            @PathVariable String settingKey,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<Void>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        blogSettingsService.deleteSetting(settingKey);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Setting deleted successfully")
                .build());
    }

    @GetMapping("/stats")
    public ResponseEntity<APIResponse<BlogStatsResponse>> getBlogStats(HttpServletRequest httpRequest) {
        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<BlogStatsResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        BlogStatsResponse response = blogSettingsService.getBlogStats();

        return ResponseEntity.ok(APIResponse.<BlogStatsResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Blog stats retrieved successfully")
                .data(response)
                .build());
    }
}