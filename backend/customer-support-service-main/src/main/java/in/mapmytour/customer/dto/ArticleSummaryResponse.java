package in.mapmytour.customer.dto;

import in.mapmytour.customer.entity.KnowledgeBaseArticle;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSummaryResponse {

    private String id;
    private String title;
    private KnowledgeBaseArticle.ArticleCategory category;
    private LocalDateTime updatedAt;
}