package in.mapmytour.blog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogStatsResponse {

    private Long totalPosts;
    private Long publishedPosts;
    private Long draftPosts;
    private Long totalComments;
    private Long approvedComments;
    private Long pendingComments;
    private Long totalLikes;
    private Long totalCategories;
    private Long totalTags;
}
