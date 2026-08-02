package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.section.CreateSectionRequest;
import in.mapmytour.blog.dto.request.section.UpdateSectionRequest;
import in.mapmytour.blog.dto.response.section.SectionResponse;
import in.mapmytour.blog.service.SectionService;
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
@RequestMapping("/api/v1/blog/sections")
@RequiredArgsConstructor
@Slf4j
public class SectionController {

    private final SectionService sectionService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<SectionResponse>> createSection(
            @Valid @RequestBody CreateSectionRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<SectionResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        SectionResponse response = sectionService.createSection(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<SectionResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Section created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<APIResponse<SectionResponse>> getSection(@PathVariable String sectionId) {
        SectionResponse response = sectionService.getSection(sectionId);

        return ResponseEntity.ok(APIResponse.<SectionResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Section retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<APIResponse<SectionResponse>> getSectionBySlug(@PathVariable String slug) {
        SectionResponse response = sectionService.getSectionBySlug(slug);

        return ResponseEntity.ok(APIResponse.<SectionResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Section retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<SectionResponse>>> getAllSections() {
        List<SectionResponse> response = sectionService.getAllSections();

        return ResponseEntity.ok(APIResponse.<List<SectionResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Sections retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<APIResponse<List<SectionResponse>>> getSectionHierarchy() {
        List<SectionResponse> response = sectionService.getSectionHierarchy();

        return ResponseEntity.ok(APIResponse.<List<SectionResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Section hierarchy retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<APIResponse<SectionResponse>> updateSection(
            @PathVariable String sectionId,
            @Valid @RequestBody UpdateSectionRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<SectionResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        SectionResponse response = sectionService.updateSection(sectionId, request);

        return ResponseEntity.ok(APIResponse.<SectionResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Section updated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<APIResponse<Void>> deleteSection(
            @PathVariable String sectionId,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<Void>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        sectionService.deleteSection(sectionId);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Section deleted successfully")
                .build());
    }
}
