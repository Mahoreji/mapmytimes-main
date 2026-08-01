package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.PaginationRequest;
import in.mapmytour.blog.dto.request.blogpost.BlogPostSearchRequest;
import in.mapmytour.blog.dto.request.blogpost.CreateBlogPostRequest;
import in.mapmytour.blog.dto.request.blogpost.UpdateBlogPostRequest;
import in.mapmytour.blog.dto.response.PaginatedResponse;
import in.mapmytour.blog.dto.response.blogpost.BlogPostResponse;
import in.mapmytour.blog.dto.response.blogpost.BlogPostSummaryResponse;
import in.mapmytour.blog.dto.response.postlike.PostLikeResponse;

import java.util.List;

public interface BlogPostService {
    BlogPostResponse createBlogPost(CreateBlogPostRequest request);
    BlogPostResponse getBlogPost(String postId);
    BlogPostResponse getBlogPostBySlug(String slug);
    PaginatedResponse<BlogPostSummaryResponse> getAllBlogPosts(PaginationRequest paginationRequest);
    PaginatedResponse<BlogPostSummaryResponse> searchBlogPosts(BlogPostSearchRequest request);
    PaginatedResponse<BlogPostSummaryResponse> getUserBlogPosts(String userId, PaginationRequest paginationRequest);
    BlogPostResponse updateBlogPost(String postId, UpdateBlogPostRequest request, String userId, boolean isAdmin);
    BlogPostResponse publishBlogPost(String postId, String userId, boolean isAdmin);
    BlogPostResponse unpublishBlogPost(String postId, String userId, boolean isAdmin);
    void deleteBlogPost(String postId, String userId, boolean isAdmin);
    
    void incrementViewCount(String postId);
    
    void likePost(String postId, String userId);
    void unlikePost(String postId, String userId);
    List<PostLikeResponse> getPostLikes(String postId);
    PaginatedResponse<BlogPostSummaryResponse> getMyLikedPosts(String userId, PaginationRequest paginationRequest);
    PaginatedResponse<BlogPostSummaryResponse> getAllBlogPostsByStatus(String status, String userId, boolean isAdmin, PaginationRequest paginationRequest);
    PaginatedResponse<BlogPostSummaryResponse> getPersonalizedFeed(List<String> userIds, List<String> postTypes, PaginationRequest paginationRequest);
}
