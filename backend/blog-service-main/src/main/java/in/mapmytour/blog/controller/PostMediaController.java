package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.postmedia.UpdateMediaRequest;
import in.mapmytour.blog.dto.request.postmedia.UploadMediaRequest;
import in.mapmytour.blog.dto.response.postmedia.PostMediaResponse;
import in.mapmytour.blog.service.PostMediaService;
import in.mapmytour.blog.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/blog/media")
@RequiredArgsConstructor
@Slf4j
public class PostMediaController {

    private final PostMediaService postMediaService;
    private final UserContextService userContextService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<PostMediaResponse>> uploadMedia(
            @RequestPart("media") String mediaJson,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        UploadMediaRequest request;
        try {
            request = objectMapper.readValue(mediaJson, UploadMediaRequest.class);
        } catch (Exception e) {
            log.error("Error parsing media JSON: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(APIResponse.<PostMediaResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message("Invalid media JSON: " + e.getMessage())
                            .build());
        }

        String userId = userContextService.getCurrentUserId(httpRequest);
        request.setUserId(userId);
        request.setMediaFile(file);

        PostMediaResponse response = postMediaService.uploadMedia(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<PostMediaResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Media uploaded successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<APIResponse<PostMediaResponse>> getMedia(@PathVariable String mediaId) {
        PostMediaResponse response = postMediaService.getMedia(mediaId);

        return ResponseEntity.ok(APIResponse.<PostMediaResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Media retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<APIResponse<List<PostMediaResponse>>> getPostMedia(@PathVariable String postId) {
        List<PostMediaResponse> response = postMediaService.getPostMedia(postId);

        return ResponseEntity.ok(APIResponse.<List<PostMediaResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Post media retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping(value = "/{mediaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<PostMediaResponse>> updateMedia(
            @PathVariable String mediaId,
            @RequestPart("media") String mediaJson,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {

        UpdateMediaRequest request;
        try {
            request = objectMapper.readValue(mediaJson, UpdateMediaRequest.class);
        } catch (Exception e) {
            log.error("Error parsing media JSON: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(APIResponse.<PostMediaResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message("Invalid media JSON: " + e.getMessage())
                            .build());
        }

        String userId = userContextService.getCurrentUserId(httpRequest);
        request.setNewMediaFile(file);

        PostMediaResponse response = postMediaService.updateMedia(mediaId, request, userId);

        return ResponseEntity.ok(APIResponse.<PostMediaResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Media updated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<APIResponse<Void>> deleteMedia(
            @PathVariable String mediaId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        boolean isAdmin = userContextService.isCurrentUserAdmin(httpRequest);

        postMediaService.deleteMedia(mediaId, userId, isAdmin);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Media deleted successfully")
                .build());
    }
}