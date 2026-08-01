package in.mapmytour.blog.dto.response.category;

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
public class CategoryResponse {

    private String id;
    private String name;
    private String slug;
    private String description;
    private String parentCategoryId;
    private List<CategoryResponse> subCategories;
    private Integer postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
