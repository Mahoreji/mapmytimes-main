package in.mapmytour.auth.controller;

import in.mapmytour.auth.dto.APIResponse;
import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.user.*;
import in.mapmytour.auth.service.TripCircleService;
import in.mapmytour.auth.service.UserContextService;
import in.mapmytour.auth.utils.APIResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller exposing Trip Circle, posts, polls and booking related endpoints.
 */
@RestController
@RequestMapping("/api/v1/circles")
@RequiredArgsConstructor
@Slf4j
public class TripCircleController {

    private final TripCircleService tripCircleService;
    private final UserContextService userContextService;

    // --------------- Circles ---------------

    @PostMapping
    public ResponseEntity<APIResponse<TripCircleResponse>> createCircle(@Valid @RequestBody TripCircleCreateRequest request,
                                                                        HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            TripCircleResponse response = tripCircleService.createCircle(request, userId);
            return APIResponseUtil.created(response, "Trip circle created successfully");
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to create circle: {}", ex.getMessage());
            return APIResponseUtil.badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while creating circle", ex);
            return APIResponseUtil.internalServerError("Failed to create circle");
        }
    }

    @GetMapping("/{circleId}")
    public ResponseEntity<APIResponse<TripCircleResponse>> getCircle(@PathVariable String circleId,
                                                                     HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            TripCircleResponse response = tripCircleService.getCircle(circleId, userId);
            return APIResponseUtil.success(response, "Circle fetched");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @GetMapping("/by-destination/{destinationId}")
    public ResponseEntity<APIResponse<List<TripCircleResponse>>> getByDestination(@PathVariable String destinationId,
                                                                                  @RequestParam(required = false) LocalDate from,
                                                                                  @RequestParam(required = false) LocalDate to,
                                                                                  HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            List<TripCircleResponse> responses = tripCircleService.getCirclesForDestination(destinationId, from, to, userId);
            return APIResponseUtil.success(responses, "Circles fetched");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @PostMapping("/{circleId}/join")
    public ResponseEntity<APIResponse<MessageResponse>> joinCircle(@PathVariable String circleId,
                                                                   HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            tripCircleService.joinCircle(circleId, userId);
            MessageResponse payload = MessageResponse.builder().message("Joined circle").build();
            return APIResponseUtil.success(payload, "Joined successfully");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @PostMapping("/{circleId}/leave")
    public ResponseEntity<APIResponse<MessageResponse>> leaveCircle(@PathVariable String circleId,
                                                                    HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            tripCircleService.leaveCircle(circleId, userId);
            MessageResponse payload = MessageResponse.builder().message("Left circle").build();
            return APIResponseUtil.success(payload, "Left successfully");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    // --------------- Posts / Feed ---------------

    @PostMapping("/{circleId}/posts/today-plan")
    public ResponseEntity<APIResponse<CirclePostResponse>> createTodayPlan(@PathVariable String circleId,
                                                                           @Valid @RequestBody TodayPlanRequest request,
                                                                           HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            CirclePostResponse response = tripCircleService.createTodayPlanPost(circleId, request, userId);
            return APIResponseUtil.created(response, "Plan posted");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @PostMapping("/{circleId}/posts/checkin")
    public ResponseEntity<APIResponse<CirclePostResponse>> createCheckin(@PathVariable String circleId,
                                                                         @Valid @RequestBody CheckinRequest request,
                                                                         HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            CirclePostResponse response = tripCircleService.createCheckinPost(circleId, request, userId);
            return APIResponseUtil.created(response, "Check-in posted");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @PostMapping("/{circleId}/posts")
    public ResponseEntity<APIResponse<CirclePostResponse>> createPost(@PathVariable String circleId,
                                                                      @Valid @RequestBody CirclePostCreateRequest request,
                                                                      HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            CirclePostResponse response = tripCircleService.createPost(circleId, request, userId);
            return APIResponseUtil.created(response, "Post created");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @GetMapping("/{circleId}/feed")
    public ResponseEntity<APIResponse<CircleFeedPageResponse>> getFeed(@PathVariable String circleId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CirclePostResponse> result = tripCircleService.getCircleFeed(circleId, userId, pageable);
            CircleFeedPageResponse body = CircleFeedPageResponse.builder()
                    .items(result.getContent())
                    .page(result.getNumber())
                    .size(result.getSize())
                    .totalElements(result.getTotalElements())
                    .last(result.isLast())
                    .build();
            return APIResponseUtil.success(body, "Feed loaded");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @DeleteMapping("/{circleId}/posts/{postId}")
    public ResponseEntity<APIResponse<MessageResponse>> deletePost(@PathVariable String circleId,
                                                                   @PathVariable String postId,
                                                                   HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            tripCircleService.deletePost(circleId, postId, userId);
            MessageResponse payload = MessageResponse.builder().message("Post deleted").build();
            return APIResponseUtil.success(payload, "Post deleted");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    // --------------- Polls ---------------

    @PostMapping("/{circleId}/polls")
    public ResponseEntity<APIResponse<CirclePollResponse>> createPoll(@PathVariable String circleId,
                                                                      @Valid @RequestBody CirclePollRequest request,
                                                                      HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            CirclePollResponse response = tripCircleService.createPoll(circleId, request, userId);
            return APIResponseUtil.created(response, "Poll created");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @GetMapping("/polls/{id}")
    public ResponseEntity<?> getPollOrPollsByCircle(@PathVariable String id,
                                                      HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            // First, try to get polls by circle ID
            List<CirclePollResponse> polls = tripCircleService.getPollsByCircle(id, userId);
            return APIResponseUtil.success(polls, "Polls fetched for circle");
        } catch (IllegalArgumentException e) {
            // If not a circle or user not a member, try as poll ID
            try {
                CirclePollResponse response = tripCircleService.getPoll(id, userId);
                return APIResponseUtil.success(response, "Poll fetched");
            } catch (IllegalArgumentException ex) {
                return APIResponseUtil.badRequest("Circle or poll not found");
            }
        }
    }

    @PostMapping("/polls/{pollId}/vote")
    public ResponseEntity<APIResponse<CirclePollResponse>> votePoll(@PathVariable String pollId,
                                                                    @RequestParam String optionId,
                                                                    HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            CirclePollResponse response = tripCircleService.votePoll(pollId, optionId, userId);
            return APIResponseUtil.success(response, "Vote recorded");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    // --------------- Booking locks & attribution helpers ---------------

    @GetMapping("/{circleId}/booking-locks")
    public ResponseEntity<APIResponse<CircleBookingLocksResponse>> getBookingLocks(@PathVariable String circleId,
                                                                                   HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            CircleBookingLocksResponse response = tripCircleService.getBookingLocks(circleId, userId);
            return APIResponseUtil.success(response, "Booking actions loaded");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }

    @PostMapping("/booking-click")
    public ResponseEntity<APIResponse<MessageResponse>> recordBookingClick(@Valid @RequestBody CircleBookingClickRequest request,
                                                                           HttpServletRequest httpRequest) {
        String userId = userContextService.getCurrentUserId(httpRequest);
        if (userId == null) {
            return APIResponseUtil.unauthorized("Authentication required");
        }
        try {
            tripCircleService.recordBookingClick(request, userId);
            MessageResponse payload = MessageResponse.builder().message("Click recorded").build();
            return APIResponseUtil.success(payload, "Click recorded");
        } catch (IllegalArgumentException ex) {
            return APIResponseUtil.badRequest(ex.getMessage());
        }
    }
}
