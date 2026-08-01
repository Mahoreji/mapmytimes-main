package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.tag.CreateTagRequest;
import in.mapmytour.blog.dto.request.tag.UpdateTagRequest;
import in.mapmytour.blog.dto.response.tag.TagResponse;

import java.util.List;

public interface TagService {
    TagResponse createTag(CreateTagRequest request);
    TagResponse getTag(String tagId);
    TagResponse getTagBySlug(String slug);
    List<TagResponse> getAllTags();
    List<TagResponse> getPopularTags(Integer limit);
    TagResponse updateTag(String tagId, UpdateTagRequest request);
    void deleteTag(String tagId);
}