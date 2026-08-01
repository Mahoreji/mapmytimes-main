package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.*;
import in.mapmytour.customer.entity.KnowledgeBaseArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface KnowledgeBaseService {
    ArticleResponse createArticle(CreateArticleRequest request);
    ArticleResponse getArticleById(String id);
    Page<ArticleSummaryResponse> searchArticles(String query, Pageable pageable);
    Page<ArticleSummaryResponse> getAllArticles(Pageable pageable, String category, boolean publishedOnly);
    ArticleResponse updateArticle(String id, UpdateArticleRequest request);
    void deleteArticle(String id);
    List<KnowledgeBaseArticle.ArticleCategory> getAllCategories();
}