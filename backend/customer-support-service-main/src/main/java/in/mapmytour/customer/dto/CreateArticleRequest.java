package in.mapmytour.customer.dto;

import in.mapmytour.customer.entity.KnowledgeBaseArticle.ArticleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateArticleRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private Set<String> keywords;

    @NotNull
    private ArticleCategory category;

    private boolean isPublished;
}