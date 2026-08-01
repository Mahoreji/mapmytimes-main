package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.tag.CreateTagRequest;
import in.mapmytour.blog.dto.request.tag.UpdateTagRequest;
import in.mapmytour.blog.dto.response.tag.TagResponse;
import in.mapmytour.blog.entity.Tag;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.exception.DuplicateResourceException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.repository.TagRepository;
import in.mapmytour.blog.service.TagService;
import in.mapmytour.blog.utils.BlogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final BlogMapper blogMapper;

    @Override
    public TagResponse createTag(CreateTagRequest request) {
        log.info("Creating tag with name: {}", request.getName());

        // Check if tag name already exists
        if (tagRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Tag with name '" + request.getName() + "' already exists");
        }

        // Check if tag slug already exists
        if (tagRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Tag with slug '" + request.getSlug() + "' already exists");
        }

        try {
            Tag tag = Tag.builder()
                    .name(request.getName())
                    .slug(request.getSlug())
                    .build();

            Tag savedTag = tagRepository.save(tag);
            log.info("Tag created successfully with ID: {}", savedTag.getId());

            return blogMapper.toTagResponse(savedTag);
        } catch (Exception e) {
            log.error("Error creating tag: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create tag: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTag(String tagId) {
        log.info("Fetching tag with ID: {}", tagId);

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with ID: " + tagId));

        TagResponse response = blogMapper.toTagResponse(tag);

        // Add post count
        try {
            Object[] result = tagRepository.findTagWithPostCount(tagId);
            if (result != null && result.length > 1) {
                response.setPostCount(((Number) result[1]).intValue());
            }
        } catch (Exception e) {
            log.warn("Error fetching post count for tag {}: {}", tagId, e.getMessage());
            response.setPostCount(0);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagBySlug(String slug) {
        log.info("Fetching tag with slug: {}", slug);

        Tag tag = tagRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with slug: " + slug));

        TagResponse response = blogMapper.toTagResponse(tag);

        // Add post count
        try {
            Object[] result = tagRepository.findTagWithPostCount(tag.getId());
            if (result != null && result.length > 1) {
                response.setPostCount(((Number) result[1]).intValue());
            }
        } catch (Exception e) {
            log.warn("Error fetching post count for tag {}: {}", tag.getId(), e.getMessage());
            response.setPostCount(0);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        log.info("Fetching all tags");

        List<Tag> tags = tagRepository.findAll();

        // Get post counts for all tags
        Map<String, Integer> postCounts = new HashMap<>();
        try {
            List<Object[]> tagPostCounts = tagRepository.findAllTagsWithPostCount();
            for (Object[] result : tagPostCounts) {
                String tagId = (String) result[0];
                Integer postCount = ((Number) result[2]).intValue();
                postCounts.put(tagId, postCount);
            }
        } catch (Exception e) {
            log.warn("Error fetching post counts for tags: {}", e.getMessage());
        }

        return tags.stream()
                .map(tag -> {
                    TagResponse response = blogMapper.toTagResponse(tag);
                    response.setPostCount(postCounts.getOrDefault(tag.getId(), 0));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getPopularTags(Integer limit) {
        log.info("Fetching popular tags with limit: {}", limit);

        List<Tag> tags = tagRepository.findAll();

        // Get post counts for all tags
        Map<String, Integer> postCounts = new HashMap<>();
        try {
            List<Object[]> tagPostCounts = tagRepository.findAllTagsWithPostCount();
            for (Object[] result : tagPostCounts) {
                String tagId = (String) result[0];
                Integer postCount = ((Number) result[2]).intValue();
                postCounts.put(tagId, postCount);
            }
        } catch (Exception e) {
            log.warn("Error fetching post counts for tags: {}", e.getMessage());
        }

        return tags.stream()
                .map(tag -> {
                    TagResponse response = blogMapper.toTagResponse(tag);
                    response.setPostCount(postCounts.getOrDefault(tag.getId(), 0));
                    return response;
                })
                .sorted((a, b) -> b.getPostCount().compareTo(a.getPostCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public TagResponse updateTag(String tagId, UpdateTagRequest request) {
        log.info("Updating tag with ID: {}", tagId);

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with ID: " + tagId));

        try {
            // Update fields if provided
            if (StringUtils.hasText(request.getName()) && !request.getName().equals(tag.getName())) {
                if (tagRepository.existsByName(request.getName())) {
                    throw new DuplicateResourceException("Tag with name '" + request.getName() + "' already exists");
                }
                tag.setName(request.getName());
            }

            if (StringUtils.hasText(request.getSlug()) && !request.getSlug().equals(tag.getSlug())) {
                if (tagRepository.existsBySlug(request.getSlug())) {
                    throw new DuplicateResourceException("Tag with slug '" + request.getSlug() + "' already exists");
                }
                tag.setSlug(request.getSlug());
            }

            Tag updatedTag = tagRepository.save(tag);
            log.info("Tag updated successfully with ID: {}", updatedTag.getId());

            return blogMapper.toTagResponse(updatedTag);
        } catch (Exception e) {
            log.error("Error updating tag: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update tag: " + e.getMessage());
        }
    }

    @Override
    public void deleteTag(String tagId) {
        log.info("Deleting tag with ID: {}", tagId);

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with ID: " + tagId));

        try {
            // Check if tag is being used in blog posts
            try {
                Object[] result = tagRepository.findTagWithPostCount(tagId);
                if (result != null && result.length > 1) {
                    Integer postCount = ((Number) result[1]).intValue();
                    if (postCount > 0) {
                        throw new BadRequestException("Cannot delete tag that is being used in blog posts. Please remove tag from posts first.");
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking post count for tag deletion: {}", e.getMessage());
            }

            tagRepository.delete(tag);
            log.info("Tag deleted successfully with ID: {}", tagId);
        } catch (Exception e) {
            log.error("Error deleting tag: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete tag: " + e.getMessage());
        }
    }
}