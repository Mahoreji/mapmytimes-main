package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.readingprogress.CreateHighlightRequest;
import in.mapmytour.blog.dto.response.readingprogress.HighlightResponse;
import in.mapmytour.blog.entity.BlogPost;
import in.mapmytour.blog.entity.Highlight;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.exception.ForbiddenException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.repository.BlogPostRepository;
import in.mapmytour.blog.repository.HighlightRepository;
import in.mapmytour.blog.service.HighlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HighlightServiceImpl implements HighlightService {

    private final HighlightRepository highlightRepository;
    private final BlogPostRepository blogPostRepository;

    @Override
    public HighlightResponse create(String userId, CreateHighlightRequest request) {
        log.info("Creating highlight for user {} on post {}", userId, request.getPostId());

        BlogPost blogPost = blogPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with ID: " + request.getPostId()));

        if (request.getCharEnd() < request.getCharStart()) {
            throw new BadRequestException("charEnd must be greater than or equal to charStart");
        }

        if (request.getParagraphIndex() < 0) {
            throw new BadRequestException("paragraphIndex must be non-negative");
        }

        Highlight highlight = Highlight.builder()
                .userId(userId)
                .post(blogPost)
                .paragraphIndex(request.getParagraphIndex())
                .charStart(request.getCharStart())
                .charEnd(request.getCharEnd())
                .excerpt(request.getExcerpt())
                .build();

        Highlight saved = highlightRepository.save(highlight);
        log.info("Highlight created with ID: {}", saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HighlightResponse> listForPost(String userId, String postId) {
        log.info("Listing highlights for user {} on post {}", userId, postId);

        if (!blogPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Blog post not found with ID: " + postId);
        }

        List<Highlight> highlights = highlightRepository.findByUserIdAndPostIdOrderByCreatedAtAsc(userId, postId);

        return highlights.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String userId, String highlightId) {
        log.info("Deleting highlight {} for user {}", highlightId, userId);

        Highlight highlight = highlightRepository.findById(highlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Highlight not found with ID: " + highlightId));

        if (!highlight.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to delete this highlight");
        }

        highlightRepository.delete(highlight);
        log.info("Highlight {} deleted successfully", highlightId);
    }

    private HighlightResponse toResponse(Highlight highlight) {
        return HighlightResponse.builder()
                .id(highlight.getId())
                .postId(highlight.getPost().getId())
                .paragraphIndex(highlight.getParagraphIndex())
                .charStart(highlight.getCharStart())
                .charEnd(highlight.getCharEnd())
                .excerpt(highlight.getExcerpt())
                .createdAt(highlight.getCreatedAt())
                .build();
    }
}
