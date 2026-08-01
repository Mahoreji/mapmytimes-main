package in.mapmytour.blog.controller;

import in.mapmytour.blog.dto.APIResponse;
import in.mapmytour.blog.service.AdminMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blog/admin")
@RequiredArgsConstructor
public class AdminMaintenanceController {

    private final AdminMaintenanceService adminMaintenanceService;

    @DeleteMapping("/posts")
    public ResponseEntity<APIResponse<Void>> deleteAllBlogPosts() {
        adminMaintenanceService.purgeAllBlogPosts();
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("All blog posts deleted successfully")
                .build());
    }
}

