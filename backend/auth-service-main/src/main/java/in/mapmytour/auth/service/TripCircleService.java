package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for Trip Circles, posts, polls and booking attribution.
 */
public interface TripCircleService {

    // Circles
    TripCircleResponse createCircle(TripCircleCreateRequest request, String currentUserId);

    TripCircleResponse getCircle(String circleId, String currentUserId);

    List<TripCircleResponse> getCirclesForDestination(String destinationId,
                                                      LocalDate from,
                                                      LocalDate to,
                                                      String currentUserId);

    void joinCircle(String circleId, String currentUserId);

    void leaveCircle(String circleId, String currentUserId);

    // Posts / feed
    CirclePostResponse createTodayPlanPost(String circleId, TodayPlanRequest request, String currentUserId);

    CirclePostResponse createCheckinPost(String circleId, CheckinRequest request, String currentUserId);

    CirclePostResponse createPost(String circleId, CirclePostCreateRequest request, String currentUserId);

    Page<CirclePostResponse> getCircleFeed(String circleId, String currentUserId, Pageable pageable);

    void deletePost(String circleId, String postId, String currentUserId);

    // Polls
    CirclePollResponse createPoll(String circleId, CirclePollRequest request, String currentUserId);

    CirclePollResponse getPoll(String pollId, String currentUserId);
    
    List<CirclePollResponse> getPollsByCircle(String circleId, String currentUserId);

    CirclePollResponse votePoll(String pollId, String optionId, String currentUserId);

    // Booking locks & attribution
    CircleBookingLocksResponse getBookingLocks(String circleId, String currentUserId);

    void recordBookingClick(CircleBookingClickRequest request, String currentUserId);

    BookingAttributionResponse recordBookingAttribution(BookingAttributionRequest request);
}
