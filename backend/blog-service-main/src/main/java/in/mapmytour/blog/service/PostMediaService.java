package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.postmedia.UpdateMediaRequest;
import in.mapmytour.blog.dto.request.postmedia.UploadMediaRequest;
import in.mapmytour.blog.dto.response.postmedia.PostMediaResponse;

import java.util.List;

public interface PostMediaService {
    PostMediaResponse uploadMedia(UploadMediaRequest request);
    PostMediaResponse getMedia(String mediaId);
    List<PostMediaResponse> getPostMedia(String postId);
    PostMediaResponse updateMedia(String mediaId, UpdateMediaRequest request, String userId);
    void deleteMedia(String mediaId, String userId, boolean isAdmin);
}