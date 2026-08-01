package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.category.CreateCategoryRequest;
import in.mapmytour.blog.dto.request.category.UpdateCategoryRequest;
import in.mapmytour.blog.dto.response.category.CategoryResponse;
import in.mapmytour.blog.entity.Category;
import in.mapmytour.blog.exception.DuplicateResourceException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.repository.CategoryRepository;
import in.mapmytour.blog.service.CategoryService;
import in.mapmytour.blog.utils.BlogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BlogMapper blogMapper;

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating category with name: {}", request.getName());

        // Check if category name already exists
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }

        // Check if category slug already exists
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Category with slug '" + request.getSlug() + "' already exists");
        }

        // Validate parent category if provided
        if (StringUtils.hasText(request.getParentCategoryId())) {
            if (!categoryRepository.existsById(request.getParentCategoryId())) {
                throw new ResourceNotFoundException("Parent category not found with ID: " + request.getParentCategoryId());
            }
        }

        try {
            Category category = Category.builder()
                    .name(request.getName())
                    .slug(request.getSlug())
                    .description(request.getDescription())
                    .parentCategoryId(request.getParentCategoryId())
                    .build();

            Category savedCategory = categoryRepository.save(category);
            log.info("Category created successfully with ID: {}", savedCategory.getId());

            return blogMapper.toCategoryResponse(savedCategory);
        } catch (Exception e) {
            log.error("Error creating category: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create category: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(String categoryId) {
        log.info("Fetching category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        CategoryResponse response = blogMapper.toCategoryResponse(category);

        // Add post count
        try {
            Object[] result = categoryRepository.findCategoryWithPostCount(categoryId);
            if (result != null && result.length > 1) {
                response.setPostCount(((Number) result[1]).intValue());
            }
        } catch (Exception e) {
            log.warn("Error fetching post count for category {}: {}", categoryId, e.getMessage());
            response.setPostCount(0);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        log.info("Fetching category with slug: {}", slug);

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));

        CategoryResponse response = blogMapper.toCategoryResponse(category);

        // Add post count
        try {
            Object[] result = categoryRepository.findCategoryWithPostCount(category.getId());
            if (result != null && result.length > 1) {
                response.setPostCount(((Number) result[1]).intValue());
            }
        } catch (Exception e) {
            log.warn("Error fetching post count for category {}: {}", category.getId(), e.getMessage());
            response.setPostCount(0);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");

        List<Category> categories = categoryRepository.findAll();

        // Get post counts for all categories
        Map<String, Integer> postCounts = new HashMap<>();
        try {
            List<Object[]> categoryPostCounts = categoryRepository.findAllCategoriesWithPostCount();
            for (Object[] result : categoryPostCounts) {
                String categoryId = (String) result[0];
                Integer postCount = ((Number) result[2]).intValue();
                postCounts.put(categoryId, postCount);
            }
        } catch (Exception e) {
            log.warn("Error fetching post counts for categories: {}", e.getMessage());
        }

        return categories.stream()
                .map(category -> {
                    CategoryResponse response = blogMapper.toCategoryResponse(category);
                    response.setPostCount(postCounts.getOrDefault(category.getId(), 0));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryHierarchy() {
        log.info("Fetching category hierarchy");

        List<Category> allCategories = categoryRepository.findAll();

        // Get post counts
        Map<String, Integer> postCounts = new HashMap<>();
        try {
            List<Object[]> categoryPostCounts = categoryRepository.findAllCategoriesWithPostCount();
            for (Object[] result : categoryPostCounts) {
                String categoryId = (String) result[0];
                Integer postCount = ((Number) result[2]).intValue();
                postCounts.put(categoryId, postCount);
            }
        } catch (Exception e) {
            log.warn("Error fetching post counts for categories: {}", e.getMessage());
        }

        // Build hierarchy
        Map<String, CategoryResponse> categoryMap = new HashMap<>();

        // First pass: create all category responses
        for (Category category : allCategories) {
            CategoryResponse response = blogMapper.toCategoryResponse(category);
            response.setPostCount(postCounts.getOrDefault(category.getId(), 0));
            response.setSubCategories(new ArrayList<>());
            categoryMap.put(category.getId(), response);
        }

        // Second pass: organize hierarchy
        List<CategoryResponse> rootCategories = new ArrayList<>();
        for (CategoryResponse category : categoryMap.values()) {
            if (category.getParentCategoryId() == null) {
                rootCategories.add(category);
            } else {
                CategoryResponse parent = categoryMap.get(category.getParentCategoryId());
                if (parent != null) {
                    parent.getSubCategories().add(category);
                }
            }
        }

        return rootCategories;
    }

    @Override
    public CategoryResponse updateCategory(String categoryId, UpdateCategoryRequest request) {
        log.info("Updating category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        try {
            // Update fields if provided
            if (StringUtils.hasText(request.getName()) && !request.getName().equals(category.getName())) {
                if (categoryRepository.existsByName(request.getName())) {
                    throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
                }
                category.setName(request.getName());
            }

            if (StringUtils.hasText(request.getSlug()) && !request.getSlug().equals(category.getSlug())) {
                if (categoryRepository.existsBySlug(request.getSlug())) {
                    throw new DuplicateResourceException("Category with slug '" + request.getSlug() + "' already exists");
                }
                category.setSlug(request.getSlug());
            }

            if (request.getDescription() != null) {
                category.setDescription(request.getDescription());
            }

            if (request.getParentCategoryId() != null) {
                // Validate parent category exists
                if (StringUtils.hasText(request.getParentCategoryId()) &&
                        !categoryRepository.existsById(request.getParentCategoryId())) {
                    throw new ResourceNotFoundException("Parent category not found with ID: " + request.getParentCategoryId());
                }

                // Prevent circular reference
                if (request.getParentCategoryId().equals(categoryId)) {
                    throw new BadRequestException("Category cannot be its own parent");
                }

                category.setParentCategoryId(request.getParentCategoryId());
            }

            Category updatedCategory = categoryRepository.save(category);
            log.info("Category updated successfully with ID: {}", updatedCategory.getId());

            return blogMapper.toCategoryResponse(updatedCategory);
        } catch (Exception e) {
            log.error("Error updating category: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update category: " + e.getMessage());
        }
    }

    @Override
    public void deleteCategory(String categoryId) {
        log.info("Deleting category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        try {
            // Check if category has subcategories
            List<Category> subCategories = categoryRepository.findByParentCategoryId(categoryId);
            if (!subCategories.isEmpty()) {
                throw new BadRequestException("Cannot delete category with subcategories. Please delete or reassign subcategories first.");
            }

            // Check if category is being used in blog posts
            try {
                Object[] result = categoryRepository.findCategoryWithPostCount(categoryId);
                if (result != null && result.length > 1) {
                    Integer postCount = ((Number) result[1]).intValue();
                    if (postCount > 0) {
                        throw new BadRequestException("Cannot delete category that is being used in blog posts. Please reassign posts to another category first.");
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking post count for category deletion: {}", e.getMessage());
            }

            categoryRepository.delete(category);
            log.info("Category deleted successfully with ID: {}", categoryId);
        } catch (Exception e) {
            log.error("Error deleting category: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete category: " + e.getMessage());
        }
    }
}