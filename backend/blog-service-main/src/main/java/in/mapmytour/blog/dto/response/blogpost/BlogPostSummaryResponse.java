package in.mapmytour.blog.dto.response.blogpost;

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
public class BlogPostSummaryResponse {

    private String id;
    private String title;
    private String slug;
    private String excerpt;
    private String status;
    private Long viewCount;
    private String userId;
    private List<String> categories;
    private List<String> tags;
    private String sectionSlug;
    private String postType;
    private Integer likeCount;
    private Integer commentCount;
    private String featuredImageUrl;
    private String primaryVideoUrl;
    private List<PostMediaResponse> media;
    private String destination;
    private String authorEmail;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
