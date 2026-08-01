package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.blogpost.MediaGroupRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import in.mapmytour.blog.entity.PostMedia;

public interface AsyncMediaUploadService {
    CompletableFuture<List<PostMedia>> uploadGroupedMediaFilesAsync(String postId, 
                                                         List<MediaGroupRequest> mediaGroups, 
                                                         List<MultipartFile> allMediaFiles, 
                                                         String userId);
    
    CompletableFuture<List<PostMedia>> uploadMediaFilesAsync(String postId, 
                                                   List<MultipartFile> mediaFiles, 
                                                   List<String> captions, 
                                                   List<String> descriptions, 
                                                   List<String> subtitles, 
                                                   String userId);
}


