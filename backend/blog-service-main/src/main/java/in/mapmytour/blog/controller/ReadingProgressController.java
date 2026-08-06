package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.readingprogress.UpsertProgressRequest;
import in.mapmytour.blog.dto.response.readingprogress.ReadingProgressResponse;
import in.mapmytour.blog.dto.response.readingprogress.ReadingProgressWithPostSummaryResponse;
import in.mapmytour.blog.exception.UnauthorizedException;
import in.mapmytour.blog.service.ReadingProgressService;
import in.mapmytour.blog.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reading-progress")
@RequiredArgsConstructor
@Slf4j
public class ReadingProgressController {

    private final ReadingProgressService readingProgressService;
    private final UserContextService userContextService;

    @PostMapping("/me")
    public ResponseEntity<APIResponse<ReadingProgressResponse>> upsertProgress(
            @Valid @RequestBody UpsertProgressRequest request,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        ReadingProgressResponse response = readingProgressService.upsert(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<ReadingProgressResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Reading progress saved successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/me/post/{postId}")
    public ResponseEntity<APIResponse<ReadingProgressResponse>> getProgressForPost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        return readingProgressService.getByPostId(userId, postId)
                .map(progress -> ResponseEntity.ok(APIResponse.<ReadingProgressResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Reading progress retrieved successfully")
                        .data(progress)
                        .build()))
                .orElseGet(() -> ResponseEntity.ok(APIResponse.<ReadingProgressResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("No reading progress found for this post")
                        .data(null)
                        .build()));
    }

    @GetMapping("/me/latest")
    public ResponseEntity<APIResponse<List<ReadingProgressWithPostSummaryResponse>>> getLatestProgress(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;

        List<ReadingProgressWithPostSummaryResponse> response = readingProgressService.getLatest(userId, limit);

        return ResponseEntity.ok(APIResponse.<List<ReadingProgressWithPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Latest reading progress retrieved successfully")
                .data(response)
                .build());
    }
}
