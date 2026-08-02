package in.mapmytour.blog.dto.response.blogpost;

import com.fasterxml.jackson.databind.JsonNode;
import in.mapmytour.blog.dto.response.postcomment.PostCommentResponse;
import in.mapmytour.blog.dto.response.postmedia.PostMediaResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostResponse {

    private String id;
    private String title;
    private String content;
    private String slug;
    private String excerpt;
    private Integer readingTime;
    private JsonNode featuredImage;
    private String primaryVideoUrl;
    private JsonNode contentBlocks;
    private JsonNode tableOfContents;
    private JsonNode travelMeta;
    private JsonNode seo;
    private String status;
    private String visibility;
    private String language;
    private String userId;
    private List<String> categories;
    private List<String> tags;
    private String sectionSlug;
    private Long viewCount;
    private Long shareCount;
    private Long bookmarkCount;
    private Boolean allowComments;
    private Boolean allowLikes;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isFeatured;
    private Boolean isTrending;
    private String postType;
    private List<PostCommentResponse> comments;
    private List<PostMediaResponse> media;
    private List<BlogPostSummaryResponse> relatedPosts;
    private String authorEmail;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime scheduledAt;
}
