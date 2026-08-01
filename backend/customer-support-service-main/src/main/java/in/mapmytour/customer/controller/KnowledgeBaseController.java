package in.mapmytour.customer.controller;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.exception.AccessDeniedException;
import in.mapmytour.customer.service.KnowledgeBaseService;
import in.mapmytour.customer.service.UserContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/knowledge-base")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<ArticleResponse>> createArticle(
            @Valid @RequestBody CreateArticleRequest request) {
        log.info("Creating article with title: {}", request.getTitle());

        // Only admin users can create articles
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can create knowledge base articles");
        }

        ArticleResponse response = knowledgeBaseService.createArticle(request);

        log.info("Article created successfully with ID: {}", response.getId());

        return ResponseEntity.ok(APIResponse.<ArticleResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Article created successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<ArticleResponse>> getArticleById(
            @PathVariable String id) {
        log.info("Fetching article with ID: {}", id);

        ArticleResponse response = knowledgeBaseService.getArticleById(id);

        return ResponseEntity.ok(APIResponse.<ArticleResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Article retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<Page<ArticleSummaryResponse>>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "true") boolean publishedOnly) {

        log.info("Fetching articles - page: {}, size: {}, category: {}", page, size, category);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortBy);

        Page<ArticleSummaryResponse> response = knowledgeBaseService.getAllArticles(
                PageRequest.of(page, size, sort), category, publishedOnly);

        return ResponseEntity.ok(APIResponse.<Page<ArticleSummaryResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Articles retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<APIResponse<Page<ArticleSummaryResponse>>> searchArticles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Searching articles with query: {}", query);

        Page<ArticleSummaryResponse> response = knowledgeBaseService.searchArticles(
                query, PageRequest.of(page, size));

        return ResponseEntity.ok(APIResponse.<Page<ArticleSummaryResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Search results retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<ArticleResponse>> updateArticle(
            @PathVariable String id,
            @Valid @RequestBody UpdateArticleRequest request) {

        log.info("Updating article with ID: {}", id);

        // Only admin users can update articles
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can update knowledge base articles");
        }

        ArticleResponse response = knowledgeBaseService.updateArticle(id, request);

        return ResponseEntity.ok(APIResponse.<ArticleResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Article updated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteArticle(@PathVariable String id) {
        log.info("Deleting article with ID: {}", id);

        // Only admin users can delete articles
        if (!userContextService.isCurrentUserAdmin()) {
            throw new AccessDeniedException(
                    "Only administrators can delete knowledge base articles");
        }

        knowledgeBaseService.deleteArticle(id);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Article deleted successfully")
                .build());
    }

    @GetMapping("/categories")
    public ResponseEntity<APIResponse<Object>> getCategories() {
        return ResponseEntity.ok(APIResponse.builder()
                .success(true)
                .statusCode(200)
                .message("Categories retrieved successfully")
                .data(knowledgeBaseService.getAllCategories())
                .build());
    }
}