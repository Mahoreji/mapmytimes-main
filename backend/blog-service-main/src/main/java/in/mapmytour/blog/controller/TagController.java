package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.tag.CreateTagRequest;
import in.mapmytour.blog.dto.request.tag.UpdateTagRequest;
import in.mapmytour.blog.dto.response.tag.TagResponse;
import in.mapmytour.blog.service.TagService;
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
@RequestMapping("/api/v1/blog/tags")
@RequiredArgsConstructor
@Slf4j
public class TagController {

    private final TagService tagService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<TagResponse>> createTag(
            @Valid @RequestBody CreateTagRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<TagResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        TagResponse response = tagService.createTag(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<TagResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Tag created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<APIResponse<TagResponse>> getTag(@PathVariable String tagId) {
        TagResponse response = tagService.getTag(tagId);

        return ResponseEntity.ok(APIResponse.<TagResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Tag retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<APIResponse<TagResponse>> getTagBySlug(@PathVariable String slug) {
        TagResponse response = tagService.getTagBySlug(slug);

        return ResponseEntity.ok(APIResponse.<TagResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Tag retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<TagResponse>>> getAllTags() {
        List<TagResponse> response = tagService.getAllTags();

        return ResponseEntity.ok(APIResponse.<List<TagResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Tags retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/popular")
    public ResponseEntity<APIResponse<List<TagResponse>>> getPopularTags(@RequestParam(defaultValue = "10") Integer limit) {
        List<TagResponse> response = tagService.getPopularTags(limit);

        return ResponseEntity.ok(APIResponse.<List<TagResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Popular tags retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<APIResponse<TagResponse>> updateTag(
            @PathVariable String tagId,
            @Valid @RequestBody UpdateTagRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<TagResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        TagResponse response = tagService.updateTag(tagId, request);

        return ResponseEntity.ok(APIResponse.<TagResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Tag updated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<APIResponse<Void>> deleteTag(
            @PathVariable String tagId,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<Void>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        tagService.deleteTag(tagId);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Tag deleted successfully")
                .build());
    }
}