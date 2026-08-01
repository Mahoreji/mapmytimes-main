package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.category.CreateCategoryRequest;
import in.mapmytour.blog.dto.request.category.UpdateCategoryRequest;
import in.mapmytour.blog.dto.response.category.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse getCategory(String categoryId);
    CategoryResponse getCategoryBySlug(String slug);
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getCategoryHierarchy();
    CategoryResponse updateCategory(String categoryId, UpdateCategoryRequest request);
    void deleteCategory(String categoryId);
}