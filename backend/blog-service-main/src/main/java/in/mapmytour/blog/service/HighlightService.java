package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.readingprogress.CreateHighlightRequest;
import in.mapmytour.blog.dto.response.readingprogress.HighlightResponse;

import java.util.List;

public interface HighlightService {
    HighlightResponse create(String userId, CreateHighlightRequest request);
    List<HighlightResponse> listForPost(String userId, String postId);
    void delete(String userId, String highlightId);
}
