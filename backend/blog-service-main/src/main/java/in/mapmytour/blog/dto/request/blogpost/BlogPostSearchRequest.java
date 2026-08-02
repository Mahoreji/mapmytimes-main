package in.mapmytour.blog.dto.request.blogpost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostSearchRequest {

    private String keyword;
    private List<String> categories;
    private List<String> tags;
    private String sectionSlug;
    private String status;
    private String userId;
    private String postType;
    private Boolean isFeatured;
    private Boolean isTrending;
    private String language;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
