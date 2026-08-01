package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.postlike.LikePostRequest;
import in.mapmytour.blog.dto.response.postlike.PostLikeResponse;
import in.mapmytour.blog.service.PostLikeService;
import in.mapmytour.blog.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blog/likes")
@RequiredArgsConstructor
@Slf4j
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final UserContextService userContextService;

    @PostMapping("/{postId}")
    public ResponseEntity<APIResponse<PostLikeResponse>> likePost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        LikePostRequest request = LikePostRequest.builder()
                .postId(postId)
                .userId(userId)
                .build();

        PostLikeResponse response = postLikeService.likePost(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<PostLikeResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Post liked successfully")
                        .data(response)
                        .build());
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<APIResponse<Void>> unlikePost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        postLikeService.unlikePost(postId, userId);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Post unliked successfully")
                .build());
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<APIResponse<List<PostLikeResponse>>> getPostLikes(@PathVariable String postId) {
        List<PostLikeResponse> response = postLikeService.getPostLikes(postId);

        return ResponseEntity.ok(APIResponse.<List<PostLikeResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Post likes retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<APIResponse<Integer>> getPostLikeCount(@PathVariable String postId) {
        Integer count = postLikeService.getPostLikeCount(postId);

        return ResponseEntity.ok(APIResponse.<Integer>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Post like count retrieved successfully")
                .data(count)
                .build());
    }

    @GetMapping("/post/{postId}/check")
    public ResponseEntity<APIResponse<Boolean>> checkUserLikedPost(
            @PathVariable String postId,
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        Boolean isLiked = postLikeService.isPostLikedByUser(postId, userId);

        return ResponseEntity.ok(APIResponse.<Boolean>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("User like status retrieved successfully")
                .data(isLiked)
                .build());
    }

    @GetMapping("/my-likes")
    public ResponseEntity<APIResponse<List<PostLikeResponse>>> getMyLikedPosts(
            HttpServletRequest httpRequest) {

        String userId = userContextService.getCurrentUserId(httpRequest);
        List<PostLikeResponse> response = postLikeService.getMyLikedPosts(userId);

        return ResponseEntity.ok(APIResponse.<List<PostLikeResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("My liked posts retrieved successfully")
                .data(response)
                .build());
    }
}