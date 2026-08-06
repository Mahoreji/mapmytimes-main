package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.readingprogress.CreateHighlightRequest;
import in.mapmytour.blog.dto.response.readingprogress.HighlightResponse;
import in.mapmytour.blog.exception.UnauthorizedException;
import in.mapmytour.blog.service.HighlightService;
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
@RequestMapping("/api/v1/highlights")
@RequiredArgsConstructor
@Slf4j
public class HighlightController {

    private final HighlightService highlightService;
    private final UserContextService userContextService;

    @PostMapping("/me")
    public ResponseEntity<APIResponse<HighlightResponse>> createHighlight(
            @Valid @RequestBody CreateHighlightRequest request,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        HighlightResponse response = highlightService.create(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<HighlightResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Highlight created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/me/post/{postId}")
    public ResponseEntity<APIResponse<List<HighlightResponse>>> getHighlightsForPost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        List<HighlightResponse> response = highlightService.listForPost(userId, postId);

        return ResponseEntity.ok(APIResponse.<List<HighlightResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Highlights retrieved successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/me/{highlightId}")
    public ResponseEntity<APIResponse<Void>> deleteHighlight(
            @PathVariable String highlightId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        highlightService.delete(userId, highlightId);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Highlight deleted successfully")
                .build());
    }
}
