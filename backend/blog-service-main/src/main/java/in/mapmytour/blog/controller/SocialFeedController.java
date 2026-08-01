package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.PaginationRequest;
import in.mapmytour.blog.dto.response.PaginatedResponse;
import in.mapmytour.blog.dto.response.blogpost.BlogPostSummaryResponse;
import in.mapmytour.blog.service.BlogPostService;
import in.mapmytour.blog.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Instagram-style social feed and discovery.
 */
@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
@Slf4j
public class SocialFeedController {

    private final BlogPostService blogPostService;
    private final UserContextService userContextService;

    /**
     * Get a personalized social feed for the current user.
     * Shows posts from connections and trending content.
     */
    @GetMapping("/feed")
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> getSocialFeed(
            @RequestParam(required = false) List<String> userIds,
            @RequestParam(defaultValue = "SOCIAL,STORY") List<String> postTypes,
            @ModelAttribute PaginationRequest paginationRequest,
            HttpServletRequest httpRequest) {
        
        String currentUserId = userContextService.getCurrentUserId(httpRequest);
        log.info("Fetching social feed for user: {}. UserIds filter: {}, PostTypes: {}", 
                currentUserId, userIds, postTypes);

        PaginatedResponse<BlogPostSummaryResponse> feed = blogPostService.getPersonalizedFeed(userIds, postTypes, paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Social feed retrieved successfully")
                .data(feed)
                .build());
    }

    /**
     * Get an explore feed with trending content from across the platform.
     */
    @GetMapping("/explore")
    public ResponseEntity<APIResponse<PaginatedResponse<BlogPostSummaryResponse>>> getExploreFeed(
            @RequestParam(defaultValue = "SOCIAL,STORY,BLOG") List<String> postTypes,
            @ModelAttribute PaginationRequest paginationRequest) {
        
        log.info("Fetching explore feed. PostTypes: {}", postTypes);
        
        PaginatedResponse<BlogPostSummaryResponse> feed = blogPostService.getPersonalizedFeed(null, postTypes, paginationRequest);

        return ResponseEntity.ok(APIResponse.<PaginatedResponse<BlogPostSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Explore feed retrieved successfully")
                .data(feed)
                .build());
    }
}
