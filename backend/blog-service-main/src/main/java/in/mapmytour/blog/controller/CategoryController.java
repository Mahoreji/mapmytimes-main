package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.dto.request.category.CreateCategoryRequest;
import in.mapmytour.blog.dto.request.category.UpdateCategoryRequest;
import in.mapmytour.blog.dto.response.category.CategoryResponse;
import in.mapmytour.blog.service.CategoryService;
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
@RequestMapping("/api/v1/blog/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;
    private final UserContextService userContextService;

    @PostMapping
    public ResponseEntity<APIResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            HttpServletRequest httpRequest) {

        // Check if user is admin
        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<CategoryResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        CategoryResponse response = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<CategoryResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Category created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<APIResponse<CategoryResponse>> getCategory(@PathVariable String categoryId) {
        CategoryResponse response = categoryService.getCategory(categoryId);

        return ResponseEntity.ok(APIResponse.<CategoryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<APIResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse response = categoryService.getCategoryBySlug(slug);

        return ResponseEntity.ok(APIResponse.<CategoryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> response = categoryService.getAllCategories();

        return ResponseEntity.ok(APIResponse.<List<CategoryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<APIResponse<List<CategoryResponse>>> getCategoryHierarchy() {
        List<CategoryResponse> response = categoryService.getCategoryHierarchy();

        return ResponseEntity.ok(APIResponse.<List<CategoryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Category hierarchy retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<APIResponse<CategoryResponse>> updateCategory(
            @PathVariable String categoryId,
            @Valid @RequestBody UpdateCategoryRequest request,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<CategoryResponse>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        CategoryResponse response = categoryService.updateCategory(categoryId, request);

        return ResponseEntity.ok(APIResponse.<CategoryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Category updated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<APIResponse<Void>> deleteCategory(
            @PathVariable String categoryId,
            HttpServletRequest httpRequest) {

        if (!userContextService.isCurrentUserAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.<Void>builder()
                            .success(false)
                            .statusCode(HttpStatus.FORBIDDEN.value())
                            .message("Admin access required")
                            .build());
        }

        categoryService.deleteCategory(categoryId);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Category deleted successfully")
                .build());
    }
}