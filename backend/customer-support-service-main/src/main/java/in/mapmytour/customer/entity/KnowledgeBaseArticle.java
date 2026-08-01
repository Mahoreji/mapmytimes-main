package in.mapmytour.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "knowledge_base_articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeBaseArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ElementCollection
    private Set<String> keywords;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleCategory category;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPublished = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ArticleCategory {
        GETTING_STARTED, TROUBLESHOOTING, FAQ, HOW_TO_GUIDE
    }

    public void setIsPublished(boolean isPublished) {
        this.isPublished = isPublished;
    }
}