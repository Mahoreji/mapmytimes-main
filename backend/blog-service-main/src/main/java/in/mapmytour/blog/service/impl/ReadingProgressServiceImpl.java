package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.readingprogress.UpsertProgressRequest;
import in.mapmytour.blog.dto.response.blogpost.BlogPostSummaryResponse;
import in.mapmytour.blog.dto.response.readingprogress.ReadingProgressResponse;
import in.mapmytour.blog.dto.response.readingprogress.ReadingProgressWithPostSummaryResponse;
import in.mapmytour.blog.entity.BlogPost;
import in.mapmytour.blog.entity.ReadingProgress;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.repository.BlogPostRepository;
import in.mapmytour.blog.repository.ReadingProgressRepository;
import in.mapmytour.blog.service.ReadingProgressService;
import in.mapmytour.blog.utils.BlogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingProgressServiceImpl implements ReadingProgressService {

    private final ReadingProgressRepository readingProgressRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogMapper blogMapper;

    @Override
    public ReadingProgressResponse upsert(String userId, UpsertProgressRequest request) {
        log.info("Upserting reading progress for user {} on post {}", userId, request.getPostId());

        if (request.getScrollPercent() < 0 || request.getScrollPercent() > 100) {
            throw new BadRequestException("Scroll percent must be between 0 and 100");
        }

        BlogPost blogPost = blogPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + request.getPostId()));

        Optional<ReadingProgress> existing = readingProgressRepository.findByUserIdAndPostId(userId, request.getPostId());

        ReadingProgress progress;
        if (existing.isPresent()) {
            progress = existing.get();
            progress.setScrollPercent(request.getScrollPercent());
        } else {
            progress = ReadingProgress.builder()
                    .post(blogPost)
                    .userId(userId)
                    .scrollPercent(request.getScrollPercent())
                    .build();
        }

        ReadingProgress saved = readingProgressRepository.save(progress);
        log.info("Reading progress saved with ID: {}", saved.getId());

        return ReadingProgressResponse.builder()
                .postId(saved.getPost().getId())
                .scrollPercent(saved.getScrollPercent())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReadingProgressResponse> getByPostId(String userId, String postId) {
        log.info("Fetching reading progress for user {} on post {}", userId, postId);

        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        return readingProgressRepository.findByUserIdAndPostId(userId, postId)
                .map(progress -> ReadingProgressResponse.builder()
                        .postId(progress.getPost().getId())
                        .scrollPercent(progress.getScrollPercent())
                        .updatedAt(progress.getUpdatedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingProgressWithPostSummaryResponse> getLatest(String userId, int limit) {
        log.info("Fetching latest reading progress for user {} with limit {}", userId, limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<ReadingProgress> progresses = readingProgressRepository.findTopByUserIdOrderByUpdatedAtDesc(userId, pageable);

        List<ReadingProgress> filtered = progresses.stream()
                .filter(p -> p.getScrollPercent() >= 5 && p.getScrollPercent() <= 95)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, ReadingProgress> progressMap = new HashMap<>();
        for (ReadingProgress rp : filtered) {
            progressMap.put(rp.getPost().getId(), rp);
        }

        List<String> postIds = new ArrayList<>(progressMap.keySet());

        List<BlogPost> posts = blogPostRepository.findAllById(postIds);

        posts.sort((a, b) -> {
            LocalDateTime t1 = progressMap.get(a.getId()).getUpdatedAt();
            LocalDateTime t2 = progressMap.get(b.getId()).getUpdatedAt();
            return t2.compareTo(t1);
        });

        return posts.stream()
                .map(post -> {
                    BlogPostSummaryResponse summary = blogMapper.toBlogPostSummaryResponse(post);
                    ReadingProgress rp = progressMap.get(post.getId());
                    return ReadingProgressWithPostSummaryResponse.builder()
                            .id(summary.getId())
                            .title(summary.getTitle())
                            .slug(summary.getSlug())
                            .excerpt(summary.getExcerpt())
                            .status(summary.getStatus())
                            .viewCount(summary.getViewCount())
                            .userId(summary.getUserId())
                            .categories(summary.getCategories())
                            .tags(summary.getTags())
                            .sectionSlug(summary.getSectionSlug())
                            .postType(summary.getPostType())
                            .likeCount(summary.getLikeCount())
                            .commentCount(summary.getCommentCount())
                            .featuredImageUrl(summary.getFeaturedImageUrl())
                            .primaryVideoUrl(summary.getPrimaryVideoUrl())
                            .media(summary.getMedia())
                            .destination(summary.getDestination())
                            .authorEmail(summary.getAuthorEmail())
                            .authorFirstName(summary.getAuthorFirstName())
                            .authorLastName(summary.getAuthorLastName())
                            .authorAvatarUrl(summary.getAuthorAvatarUrl())
                            .createdAt(summary.getCreatedAt())
                            .publishedAt(summary.getPublishedAt())
                            .readingTimeMinutes(post.getReadingTime() != null ? post.getReadingTime() : 0)
                            .scrollPercent(rp.getScrollPercent())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
