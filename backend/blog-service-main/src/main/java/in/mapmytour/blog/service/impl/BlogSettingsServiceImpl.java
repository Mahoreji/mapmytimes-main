package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.blogsettings.CreateSettingRequest;
import in.mapmytour.blog.dto.request.blogsettings.UpdateSettingRequest;
import in.mapmytour.blog.dto.response.BlogStatsResponse;
import in.mapmytour.blog.dto.response.blogsettings.BlogSettingsResponse;
import in.mapmytour.blog.entity.BlogSettings;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.exception.DuplicateResourceException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.repository.BlogPostRepository;
import in.mapmytour.blog.repository.BlogSettingsRepository;
import in.mapmytour.blog.repository.CategoryRepository;
import in.mapmytour.blog.repository.PostCommentRepository;
import in.mapmytour.blog.repository.PostLikeRepository;
import in.mapmytour.blog.repository.TagRepository;
import in.mapmytour.blog.service.BlogSettingsService;
import in.mapmytour.blog.utils.BlogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogSettingsServiceImpl implements BlogSettingsService {

    private final BlogSettingsRepository blogSettingsRepository;
    private final BlogPostRepository blogPostRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostLikeRepository postLikeRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final BlogMapper blogMapper;

    @Override
    public BlogSettingsResponse createSetting(CreateSettingRequest request) {
        log.info("Creating blog setting with key: {}", request.getSettingKey());

        // Check if setting key already exists
        if (blogSettingsRepository.existsBySettingKey(request.getSettingKey())) {
            throw new DuplicateResourceException("Setting with key '" + request.getSettingKey() + "' already exists");
        }

        try {
            BlogSettings settings = BlogSettings.builder()
                    .settingKey(request.getSettingKey())
                    .settingValue(request.getSettingValue())
                    .build();

            BlogSettings savedSettings = blogSettingsRepository.save(settings);
            log.info("Blog setting created successfully with ID: {}", savedSettings.getId());

            return blogMapper.toBlogSettingsResponse(savedSettings);
        } catch (Exception e) {
            log.error("Error creating blog setting: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create blog setting: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BlogSettingsResponse getSetting(String settingKey) {
        log.info("Fetching blog setting with key: {}", settingKey);

        BlogSettings settings = blogSettingsRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + settingKey));

        return blogMapper.toBlogSettingsResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogSettingsResponse> getAllSettings() {
        log.info("Fetching all blog settings");

        List<BlogSettings> settings = blogSettingsRepository.findAll();
        return settings.stream()
                .map(blogMapper::toBlogSettingsResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getSettingsMap() {
        log.info("Fetching blog settings as map");

        List<BlogSettings> settings = blogSettingsRepository.findAll();
        return settings.stream()
                .collect(Collectors.toMap(
                        BlogSettings::getSettingKey,
                        BlogSettings::getSettingValue,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public BlogSettingsResponse updateSetting(String settingKey, UpdateSettingRequest request) {
        log.info("Updating blog setting with key: {}", settingKey);

        BlogSettings settings = blogSettingsRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + settingKey));

        try {
            settings.setSettingValue(request.getSettingValue());
            BlogSettings updatedSettings = blogSettingsRepository.save(settings);
            log.info("Blog setting updated successfully with key: {}", settingKey);

            return blogMapper.toBlogSettingsResponse(updatedSettings);
        } catch (Exception e) {
            log.error("Error updating blog setting: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update blog setting: " + e.getMessage());
        }
    }

    @Override
    public void deleteSetting(String settingKey) {
        log.info("Deleting blog setting with key: {}", settingKey);

        BlogSettings settings = blogSettingsRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + settingKey));

        try {
            blogSettingsRepository.delete(settings);
            log.info("Blog setting deleted successfully with key: {}", settingKey);
        } catch (Exception e) {
            log.error("Error deleting blog setting: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete blog setting: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BlogStatsResponse getBlogStats() {
        log.info("Fetching blog statistics");

        try {
            // Get post statistics
            long totalPosts = blogPostRepository.count();
            long publishedPosts = blogPostRepository.countByStatus("PUBLISHED");
            long draftPosts = blogPostRepository.countByStatus("DRAFT");

            // Get comment statistics
            long totalComments = postCommentRepository.count();
            long approvedComments = postCommentRepository.countByStatus("APPROVED");
            long pendingComments = postCommentRepository.countByStatus("PENDING");

            // Get like statistics
            long totalLikes = postLikeRepository.count();

            // Get category and tag statistics
            long totalCategories = categoryRepository.count();
            long totalTags = tagRepository.count();

            BlogStatsResponse stats = BlogStatsResponse.builder()
                    .totalPosts(totalPosts)
                    .publishedPosts(publishedPosts)
                    .draftPosts(draftPosts)
                    .totalComments(totalComments)
                    .approvedComments(approvedComments)
                    .pendingComments(pendingComments)
                    .totalLikes(totalLikes)
                    .totalCategories(totalCategories)
                    .totalTags(totalTags)
                    .build();

            log.info("Blog statistics fetched successfully");
            return stats;
        } catch (Exception e) {
            log.error("Error fetching blog statistics: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to fetch blog statistics: " + e.getMessage());
        }
    }
}