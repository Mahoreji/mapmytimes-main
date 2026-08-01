package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.postlike.LikePostRequest;
import in.mapmytour.blog.dto.response.postlike.PostLikeResponse;

import java.util.List;

public interface PostLikeService {
    PostLikeResponse likePost(LikePostRequest request);
    void unlikePost(String postId, String userId);
    List<PostLikeResponse> getPostLikes(String postId);
    Integer getPostLikeCount(String postId);
    Boolean isPostLikedByUser(String postId, String userId);
    List<PostLikeResponse> getMyLikedPosts(String userId);
}