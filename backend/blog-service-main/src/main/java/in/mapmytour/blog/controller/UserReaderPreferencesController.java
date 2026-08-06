package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.readingprogress.UpsertReaderPrefsRequest;
import in.mapmytour.blog.dto.response.readingprogress.UserReaderPreferencesResponse;
import in.mapmytour.blog.exception.UnauthorizedException;
import in.mapmytour.blog.service.UserContextService;
import in.mapmytour.blog.service.UserReaderPreferencesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserReaderPreferencesController {

    private final UserReaderPreferencesService userReaderPreferencesService;
    private final UserContextService userContextService;

    @PutMapping("/me/reader-preferences")
    public ResponseEntity<APIResponse<UserReaderPreferencesResponse>> upsertReaderPreferences(
            @RequestBody UpsertReaderPrefsRequest request,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        UserReaderPreferencesResponse response = userReaderPreferencesService.upsert(userId, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.<UserReaderPreferencesResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Reader preferences saved successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/me/reader-preferences")
    public ResponseEntity<APIResponse<UserReaderPreferencesResponse>> getReaderPreferences(
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        UserReaderPreferencesResponse response = userReaderPreferencesService.get(userId);

        return ResponseEntity.ok(APIResponse.<UserReaderPreferencesResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Reader preferences retrieved successfully")
                .data(response)
                .build());
    }
}
