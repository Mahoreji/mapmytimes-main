package in.mapmytour.blog.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blog_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "reading_time")
    private Integer readingTime;

    @Column(name = "featured_image", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode featuredImage;

    @Column(name = "primary_video_url", columnDefinition = "TEXT")
    private String primaryVideoUrl;

    @Column(name = "content_blocks", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode contentBlocks;

    @Column(name = "table_of_contents", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode tableOfContents;

    @Column(name = "travel_meta", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode travelMeta;

    @Column(name = "seo", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode seo;

    @Column(nullable = false)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, PUBLISHED, ARCHIVED

    @Column(nullable = false)
    @Builder.Default
    private String visibility = "PUBLIC";

    @Column(nullable = false)
    @Builder.Default
    private String language = "en";

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "share_count", nullable = false)
    @Builder.Default
    private Long shareCount = 0L;

    @Column(name = "bookmark_count", nullable = false)
    @Builder.Default
    private Long bookmarkCount = 0L;

    @Column(nullable = false)
    private String userId; // Reference to user-service

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostMedia> media = new ArrayList<>();

    @ElementCollection
    @Builder.Default
    private List<String> categories = new ArrayList<>();

    @ElementCollection
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "section_slug", length = 64)
    private String sectionSlug;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowComments = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowLikes = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_trending", nullable = false)
    @Builder.Default
    private Boolean isTrending = false;

    @Column(name = "post_type", nullable = false)
    @Builder.Default
    private String postType = "BLOG"; // BLOG, ARTICLE, VIDEO, SHORT, STORY, SOCIAL, PAGE

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "author_first_name")
    private String authorFirstName;

    @Column(name = "author_last_name")
    private String authorLastName;

    @Column(name = "author_avatar_url", columnDefinition = "TEXT")
    private String authorAvatarUrl;

    // Helper methods
    public void publish() {
        if ("DRAFT".equals(status)) {
            status = "PUBLISHED";
            publishedAt = LocalDateTime.now();
        }
    }

    public int getLikeCount() {
        return likes.size();
    }
}
