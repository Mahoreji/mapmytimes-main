package in.mapmytour.blog.utils;

import in.mapmytour.blog.dto.response.PaginatedResponse;
import in.mapmytour.blog.dto.response.blogpost.BlogPostResponse;
import in.mapmytour.blog.dto.response.blogpost.BlogPostSummaryResponse;
import in.mapmytour.blog.dto.response.blogsettings.BlogSettingsResponse;
import in.mapmytour.blog.dto.response.category.CategoryResponse;
import in.mapmytour.blog.dto.response.postcomment.PostCommentResponse;
import in.mapmytour.blog.dto.response.postlike.PostLikeResponse;
import in.mapmytour.blog.dto.response.postmedia.PostMediaResponse;
import in.mapmytour.blog.dto.response.section.SectionResponse;
import in.mapmytour.blog.dto.response.tag.TagResponse;
import in.mapmytour.blog.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import in.mapmytour.blog.dto.external.UserProfileResponse;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BlogMapper {

    public BlogPostResponse toBlogPostResponse(BlogPost blogPost) {
        return toBlogPostResponse(blogPost, null, null);
    }

    public BlogPostResponse toBlogPostResponse(BlogPost blogPost, UserProfileResponse profile) {
        return toBlogPostResponse(blogPost, profile, null);
    }

    public BlogPostResponse toBlogPostResponse(BlogPost blogPost, UserProfileResponse profile, Map<String, UserProfileResponse> commentProfileMap) {
        String authorEmail = blogPost.getAuthorEmail();
        String authorFirstName = blogPost.getAuthorFirstName();
        String authorLastName = blogPost.getAuthorLastName();
        String authorAvatarUrl = blogPost.getAuthorAvatarUrl();

        if (profile != null) {
            authorEmail = profile.getEmail();
            authorFirstName = profile.getFirstName();
            authorLastName = profile.getLastName();
            authorAvatarUrl = profile.getAvatarUrl();
        }

        return BlogPostResponse.builder()
                .id(blogPost.getId())
                .title(blogPost.getTitle())
                .content(blogPost.getContent())
                .slug(blogPost.getSlug())
                .excerpt(blogPost.getExcerpt())
                .readingTime(blogPost.getReadingTime())
                .featuredImage(blogPost.getFeaturedImage())
                .primaryVideoUrl(blogPost.getPrimaryVideoUrl())
                .contentBlocks(blogPost.getContentBlocks())
                .tableOfContents(blogPost.getTableOfContents())
                .travelMeta(blogPost.getTravelMeta())
                .seo(blogPost.getSeo())
                .status(blogPost.getStatus())
                .visibility(blogPost.getVisibility())
                .language(blogPost.getLanguage())
                .userId(blogPost.getUserId())
                .categories(blogPost.getCategories() != null ? new java.util.ArrayList<>(blogPost.getCategories()) : new java.util.ArrayList<>())
                .tags(blogPost.getTags() != null ? new java.util.ArrayList<>(blogPost.getTags()) : new java.util.ArrayList<>())
                .sectionSlug(blogPost.getSectionSlug())
                .viewCount(blogPost.getViewCount())
                .shareCount(blogPost.getShareCount())
                .bookmarkCount(blogPost.getBookmarkCount())
                .allowComments(blogPost.getAllowComments())
                .allowLikes(blogPost.getAllowLikes())
                .likeCount(blogPost.getLikes().size())
                .commentCount((int) blogPost.getComments().stream().filter(PostComment::isApproved).count())
                .isFeatured(blogPost.getIsFeatured())
                .isTrending(blogPost.getIsTrending())
                .postType(blogPost.getPostType())
                .comments(blogPost.getComments().stream()
                        .filter(PostComment::isApproved)
                        .map(comment -> toPostCommentResponse(comment, commentProfileMap))
                        .collect(Collectors.toList()))
                .media(blogPost.getMedia().stream()
                        .map(this::toPostMediaResponse)
                        .collect(Collectors.toList()))
                .authorEmail(authorEmail)
                .authorFirstName(authorFirstName)
                .authorLastName(authorLastName)
                .authorAvatarUrl(authorAvatarUrl)
                .createdAt(blogPost.getCreatedAt())
                .updatedAt(blogPost.getUpdatedAt())
                .publishedAt(blogPost.getPublishedAt())
                .scheduledAt(blogPost.getScheduledAt())
                .build();
    }

    public BlogPostSummaryResponse toBlogPostSummaryResponse(BlogPost blogPost) {
        return toBlogPostSummaryResponse(blogPost, (UserProfileResponse) null);
    }

    public BlogPostSummaryResponse toBlogPostSummaryResponse(BlogPost blogPost, UserProfileResponse profile) {
        String authorEmail = blogPost.getAuthorEmail();
        String authorFirstName = blogPost.getAuthorFirstName();
        String authorLastName = blogPost.getAuthorLastName();
        String authorAvatarUrl = blogPost.getAuthorAvatarUrl();

        if (profile != null) {
            authorEmail = profile.getEmail();
            authorFirstName = profile.getFirstName();
            authorLastName = profile.getLastName();
            authorAvatarUrl = profile.getAvatarUrl();
        }

        return BlogPostSummaryResponse.builder()
                .id(blogPost.getId())
                .title(blogPost.getTitle())
                .slug(blogPost.getSlug())
                .excerpt(blogPost.getExcerpt())
                .status(blogPost.getStatus())
                .userId(blogPost.getUserId())
                .categories(blogPost.getCategories() != null ? new java.util.ArrayList<>(blogPost.getCategories()) : new java.util.ArrayList<>())
                .tags(blogPost.getTags() != null ? new java.util.ArrayList<>(blogPost.getTags()) : new java.util.ArrayList<>())
                .sectionSlug(blogPost.getSectionSlug())
                .viewCount(blogPost.getViewCount())
                .postType(blogPost.getPostType())
                .likeCount(blogPost.getLikes().size())
                .commentCount((int) blogPost.getComments().stream().filter(PostComment::isApproved).count())
                .featuredImageUrl(blogPost.getMedia().stream()
                        .filter(media -> "image".equals(media.getMediaType()))
                        .map(PostMedia::getMediaUrl)
                        .findFirst()
                        .orElse(null))
                .primaryVideoUrl(blogPost.getPrimaryVideoUrl())
                .media(blogPost.getMedia().stream()
                        .map(this::toPostMediaResponse)
                        .collect(Collectors.toList()))
                .destination(blogPost.getTravelMeta() != null && blogPost.getTravelMeta().has("destination")
                        ? blogPost.getTravelMeta().get("destination").asText()
                        : null)
                .authorEmail(authorEmail)
                .authorFirstName(authorFirstName)
                .authorLastName(authorLastName)
                .authorAvatarUrl(authorAvatarUrl)
                .createdAt(blogPost.getCreatedAt())
                .publishedAt(blogPost.getPublishedAt())
                .build();
    }

    public BlogPostSummaryResponse toBlogPostSummaryResponse(BlogPost blogPost, Map<String, UserProfileResponse> profileMap) {
        UserProfileResponse profile = profileMap != null ? profileMap.get(blogPost.getUserId()) : null;
        return toBlogPostSummaryResponse(blogPost, profile);
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentCategoryId(category.getParentCategoryId())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public SectionResponse toSectionResponse(Section section) {
        return SectionResponse.builder()
                .id(section.getId())
                .name(section.getName())
                .slug(section.getSlug())
                .description(section.getDescription())
                .icon(section.getIcon())
                .accentColor(section.getAccentColor())
                .sortOrder(section.getSortOrder())
                .parentSectionId(section.getParentSectionId())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }

    public PostCommentResponse toPostCommentResponse(PostComment comment) {
        return toPostCommentResponse(comment, (UserProfileResponse) null);
    }

    public PostCommentResponse toPostCommentResponse(PostComment comment, UserProfileResponse profile) {
        String authorEmail = null;
        String authorFirstName = null;
        String authorLastName = null;
        String authorAvatarUrl = null;

        if (profile != null) {
            authorEmail = profile.getEmail();
            authorFirstName = profile.getFirstName();
            authorLastName = profile.getLastName();
            authorAvatarUrl = profile.getAvatarUrl();
        } else {
            // Fallback to denormalized fields in entity
            authorEmail = comment.getAuthorEmail();
            authorFirstName = comment.getAuthorFirstName();
            authorLastName = comment.getAuthorLastName();
            authorAvatarUrl = comment.getAuthorAvatarUrl();
        }

        return PostCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .content(comment.getContent())
                .userId(comment.getUserId())
                .authorEmail(authorEmail)
                .authorFirstName(authorFirstName)
                .authorLastName(authorLastName)
                .authorAvatarUrl(authorAvatarUrl)
                .parentCommentId(comment.getParentCommentId())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public PostCommentResponse toPostCommentResponse(PostComment comment, Map<String, UserProfileResponse> profileMap) {
        UserProfileResponse profile = profileMap != null ? profileMap.get(comment.getUserId()) : null;
        return toPostCommentResponse(comment, profile);
    }

    public PostLikeResponse toPostLikeResponse(PostLike like) {
        return toPostLikeResponse(like, (UserProfileResponse) null);
    }

    public PostLikeResponse toPostLikeResponse(PostLike like, UserProfileResponse profile) {
        String authorEmail = null;
        String authorFirstName = null;
        String authorLastName = null;
        String authorAvatarUrl = null;

        if (profile != null) {
            authorEmail = profile.getEmail();
            authorFirstName = profile.getFirstName();
            authorLastName = profile.getLastName();
            authorAvatarUrl = profile.getAvatarUrl();
        } else {
            // Fallback to denormalized fields in entity
            authorEmail = like.getAuthorEmail();
            authorFirstName = like.getAuthorFirstName();
            authorLastName = like.getAuthorLastName();
            authorAvatarUrl = like.getAuthorAvatarUrl();
        }

        return PostLikeResponse.builder()
                .id(like.getId())
                .postId(like.getPost().getId())
                .userId(like.getUserId())
                .authorEmail(authorEmail)
                .authorFirstName(authorFirstName)
                .authorLastName(authorLastName)
                .authorAvatarUrl(authorAvatarUrl)
                .likedAt(like.getLikedAt())
                .build();
    }

    public PostLikeResponse toPostLikeResponse(PostLike like, Map<String, UserProfileResponse> profileMap) {
        UserProfileResponse profile = profileMap != null ? profileMap.get(like.getUserId()) : null;
        return toPostLikeResponse(like, profile);
    }

    public PostMediaResponse toPostMediaResponse(PostMedia media) {
        return PostMediaResponse.builder()
                .id(media.getId())
                .postId(media.getPost().getId())
                .mediaUrl(media.getMediaUrl())
                .mediaType(media.getMediaType())
                .caption(media.getCaption())
                .description(media.getDescription())
                .subtitle(media.getSubtitle())
                .subtitleGroupIndex(media.getSubtitleGroupIndex())
                .userId(media.getUserId())
                .displayOrder(media.getDisplayOrder())
                .uploadedAt(media.getUploadedAt())
                .build();
    }

    public TagResponse toTagResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }

    public BlogSettingsResponse toBlogSettingsResponse(BlogSettings settings) {
        return BlogSettingsResponse.builder()
                .id(settings.getId())
                .settingKey(settings.getSettingKey())
                .settingValue(settings.getSettingValue())
                .build();
    }

    public <T> PaginatedResponse<T> toPaginatedResponse(Page<T> page) {
        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}
