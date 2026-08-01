package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.entity.KnowledgeBaseArticle;
import in.mapmytour.customer.exception.ResourceNotFoundException;
import in.mapmytour.customer.exception.ServiceException;
import in.mapmytour.customer.repository.KnowledgeBaseArticleRepository;
import in.mapmytour.customer.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseArticleRepository articleRepository;

    @Override
    @Transactional
    public ArticleResponse createArticle(CreateArticleRequest request) {
        try {
            log.debug("Creating article with title: {}", request.getTitle());

            // Check if article with same title already exists
            if (articleRepository.existsByTitleIgnoreCase(request.getTitle())) {
                throw new ServiceException("Article with title '" + request.getTitle() + "' already exists");
            }

            KnowledgeBaseArticle article = KnowledgeBaseArticle.builder()
                    .id(UUID.randomUUID().toString())
                    .title(request.getTitle().trim())
                    .content(request.getContent().trim())
                    .keywords(request.getKeywords())
                    .category(request.getCategory())
                    .viewCount(0)
                    .isPublished(request.isPublished())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            KnowledgeBaseArticle savedArticle = articleRepository.save(article);
            log.info("Article created successfully with ID: {}", savedArticle.getId());

            return mapToArticleResponse(savedArticle);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating article", e);
            throw new ServiceException("Failed to create article due to data integrity violation");
        } catch (Exception e) {
            log.error("Unexpected error while creating article", e);
            throw new ServiceException("Failed to create article: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ArticleResponse getArticleById(String id) {
        try {
            log.debug("Fetching article with ID: {}", id);

            KnowledgeBaseArticle article = articleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));

            // Increment view count - must be in non-read-only transaction for @Modifying query
            try {
                articleRepository.incrementViewCount(id);
            } catch (Exception e) {
                log.warn("Failed to increment view count for article {}: {}", id, e.getMessage());
                // Don't fail the request if view count increment fails
            }

            return mapToArticleResponse(article);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching article with ID: {}", id, e);
            throw new ServiceException("Failed to fetch article: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getAllArticles(Pageable pageable, String category, boolean publishedOnly) {
        try {
            log.debug("Fetching articles with category: {}, publishedOnly: {}", category, publishedOnly);

            if (StringUtils.hasText(category)) {
                try {
                    KnowledgeBaseArticle.ArticleCategory articleCategory =
                            KnowledgeBaseArticle.ArticleCategory.valueOf(category.toUpperCase());

                    if (publishedOnly) {
                        return articleRepository.findByCategoryAndIsPublished(articleCategory, true, pageable)
                                .map(this::mapToArticleSummaryResponse);
                    } else {
                        return articleRepository.findByCategory(articleCategory, pageable)
                                .map(this::mapToArticleSummaryResponse);
                    }
                } catch (IllegalArgumentException e) {
                    throw new ServiceException("Invalid article category: " + category);
                }
            }

            if (publishedOnly) {
                return articleRepository.findByIsPublished(true, pageable)
                        .map(this::mapToArticleSummaryResponse);
            } else {
                return articleRepository.findAll(pageable)
                        .map(this::mapToArticleSummaryResponse);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching articles", e);
            throw new ServiceException("Failed to fetch articles: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> searchArticles(String query, Pageable pageable) {
        try {
            log.debug("Searching articles with query: {}", query);

            if (!StringUtils.hasText(query)) {
                throw new ServiceException("Search query cannot be empty");
            }

            return articleRepository.searchPublishedArticles(query.trim(), true, pageable)
                    .map(this::mapToArticleSummaryResponse);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching articles with query: {}", query, e);
            throw new ServiceException("Failed to search articles: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ArticleResponse updateArticle(String id, UpdateArticleRequest request) {
        try {
            log.debug("Updating article with ID: {}", id);

            KnowledgeBaseArticle article = articleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));

            // Update fields if provided
            if (StringUtils.hasText(request.getTitle())) {
                // Check if new title conflicts with existing articles (excluding current one)
                String newTitle = request.getTitle().trim();
                if (!newTitle.equalsIgnoreCase(article.getTitle()) &&
                        articleRepository.existsByTitleIgnoreCase(newTitle)) {
                    throw new ServiceException("Article with title '" + newTitle + "' already exists");
                }
                article.setTitle(newTitle);
            }

            if (StringUtils.hasText(request.getContent())) {
                article.setContent(request.getContent().trim());
            }

            if (request.getKeywords() != null) {
                article.setKeywords(request.getKeywords());
            }

            if (request.getCategory() != null) {
                article.setCategory(request.getCategory());
            }

            if (request.getIsPublished() != null) {
                article.setIsPublished(request.getIsPublished());
            }

            article.setUpdatedAt(LocalDateTime.now());

            KnowledgeBaseArticle updatedArticle = articleRepository.save(article);
            log.info("Article updated successfully with ID: {}", updatedArticle.getId());

            return mapToArticleResponse(updatedArticle);
        } catch (ResourceNotFoundException | ServiceException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating article", e);
            throw new ServiceException("Failed to update article due to data integrity violation");
        } catch (Exception e) {
            log.error("Error updating article with ID: {}", id, e);
            throw new ServiceException("Failed to update article: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteArticle(String id) {
        try {
            log.debug("Deleting article with ID: {}", id);

            if (!articleRepository.existsById(id)) {
                throw new ResourceNotFoundException("Article not found with id: " + id);
            }

            articleRepository.deleteById(id);
            log.info("Article deleted successfully with ID: {}", id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting article with ID: {}", id, e);
            throw new ServiceException("Failed to delete article: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseArticle.ArticleCategory> getAllCategories() {
        try {
            return articleRepository.findAllPublishedCategories();
        } catch (Exception e) {
            log.error("Error fetching article categories", e);
            throw new ServiceException("Failed to fetch article categories: " + e.getMessage());
        }
    }

    private ArticleResponse mapToArticleResponse(KnowledgeBaseArticle article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .keywords(article.getKeywords())
                .category(article.getCategory())
                .viewCount(article.getViewCount())
                .isPublished(article.isPublished())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private ArticleSummaryResponse mapToArticleSummaryResponse(KnowledgeBaseArticle article) {
        return ArticleSummaryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .category(article.getCategory())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}