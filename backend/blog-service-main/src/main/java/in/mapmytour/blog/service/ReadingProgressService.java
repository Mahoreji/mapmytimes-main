package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.readingprogress.UpsertProgressRequest;
import in.mapmytour.blog.dto.response.readingprogress.ReadingProgressResponse;
import in.mapmytour.blog.dto.response.readingprogress.ReadingProgressWithPostSummaryResponse;

import java.util.List;
import java.util.Optional;

public interface ReadingProgressService {
    ReadingProgressResponse upsert(String userId, UpsertProgressRequest request);
    Optional<ReadingProgressResponse> getByPostId(String userId, String postId);
    List<ReadingProgressWithPostSummaryResponse> getLatest(String userId, int limit);
}
