package in.mapmytour.customer.dto;

import in.mapmytour.customer.entity.KnowledgeBaseArticle.ArticleCategory;
import lombok.*;

import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateArticleRequest {

    private String title;
    private String content;
    private Set<String> keywords;
    private ArticleCategory category;
    private Boolean isPublished;
}