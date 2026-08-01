package in.mapmytour.customer.dto;

import in.mapmytour.customer.entity.KnowledgeBaseArticle;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {

    private String id;
    private String title;
    private String content;
    private Set<String> keywords;
    private KnowledgeBaseArticle.ArticleCategory category;
    private Integer viewCount;
    private boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}