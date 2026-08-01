package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.PaginationRequest;
import in.mapmytour.blog.dto.request.postcomment.CreateCommentRequest;
import in.mapmytour.blog.dto.request.postcomment.UpdateCommentRequest;
import in.mapmytour.blog.dto.response.PaginatedResponse;
import in.mapmytour.blog.dto.response.postcomment.PostCommentResponse;

import java.util.List;

public interface PostCommentService {
    PostCommentResponse createComment(CreateCommentRequest request);
    PostCommentResponse getComment(String commentId);
    PaginatedResponse<PostCommentResponse> getAllComments(PaginationRequest paginationRequest);
    List<PostCommentResponse> getPostComments(String postId);
    List<PostCommentResponse> getApprovedPostComments(String postId);
    PaginatedResponse<PostCommentResponse> getPendingComments(PaginationRequest paginationRequest);
    PaginatedResponse<PostCommentResponse> getUserComments(String userId, PaginationRequest paginationRequest);
    PostCommentResponse updateComment(String commentId, UpdateCommentRequest request, String userId);
    PostCommentResponse approveComment(String commentId);
    PostCommentResponse rejectComment(String commentId);
    void deleteComment(String commentId, String userId, boolean isAdmin);
}