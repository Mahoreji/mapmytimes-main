package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.dto.user.*;
import in.mapmytour.auth.entity.*;
import in.mapmytour.auth.repository.*;
import in.mapmytour.auth.service.TripCircleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Implementation of trip circles, posts, polls and booking attribution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TripCircleServiceImpl implements TripCircleService {

    private final TripCircleRepository tripCircleRepository;
    private final TripCircleMemberRepository tripCircleMemberRepository;
    private final CirclePostRepository circlePostRepository;
    private final CirclePollRepository circlePollRepository;
    private final CirclePollOptionRepository circlePollOptionRepository;
    private final CirclePollVoteRepository circlePollVoteRepository;
    private final BookingAttributionRepository bookingAttributionRepository;
    private final UserActionDedupRepository userActionDedupRepository;
    private final RateLimitCounterRepository rateLimitCounterRepository;
    private final CircleLastClickRepository circleLastClickRepository;
    private final UserRepository userRepository;

    @Value("${mmt.circles.maxPostsPerHour:30}")
    private int maxPostsPerHour;

    @Value("${mmt.circles.maxCirclesPerDay:5}")
    private int maxCirclesPerDay;

    @Value("${mmt.circles.maxPollsPerDay:10}")
    private int maxPollsPerDay;

    @Value("${mmt.circles.dedupActionPerDay:true}")
    private boolean enableDedup;

    @Value("${mmt.circles.attributionLookbackDays:7}")
    private int bookingAttributionWindowDays;

    // ===================== Circles =====================

    @Override
    public TripCircleResponse createCircle(TripCircleCreateRequest request, String currentUserId) {
        validateCircleDates(request.getStartDate(), request.getEndDate());
        enforceCircleRateLimit(currentUserId);

        String title = (request.getTitle() == null || request.getTitle().isBlank())
                ? buildDefaultTitle(request)
                : request.getTitle().trim();

        TripCircle circle = TripCircle.builder()
                .destinationId(request.getDestinationId())
                .title(title)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdByUserId(currentUserId)
                .visibility(request.getVisibility() != null ? request.getVisibility() : CircleVisibility.DESTINATION_PUBLIC)
                .status(CircleStatus.ACTIVE)
                .build();

        circle = tripCircleRepository.save(circle);

        TripCircleMember member = TripCircleMember.builder()
                .circle(circle)
                .userId(currentUserId)
                .role(TripCircleMemberRole.OWNER)
                .build();
        tripCircleMemberRepository.save(member);

        return toResponse(circle, currentUserId);
    }

    @Override
    public TripCircleResponse getCircle(String circleId, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        return toResponse(circle, currentUserId);
    }

    @Override
    public List<TripCircleResponse> getCirclesForDestination(String destinationId, LocalDate from, LocalDate to, String currentUserId) {
        if (from == null) {
            from = LocalDate.now();
        }
        if (to == null) {
            to = from.plusMonths(6);
        }
        List<TripCircle> circles = tripCircleRepository
                .findByDestinationIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        destinationId, CircleStatus.ACTIVE, to, from);
        List<TripCircleResponse> responses = new ArrayList<>();
        for (TripCircle circle : circles) {
            responses.add(toResponse(circle, currentUserId));
        }
        return responses;
    }

    @Override
    public void joinCircle(String circleId, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        if (circle.getStatus() != CircleStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot join a non-active circle");
        }

        boolean exists = tripCircleMemberRepository.existsByCircleAndUserIdAndLeftAtIsNull(circle, currentUserId);
        if (exists) {
            return; // already a member
        }

        TripCircleMember member = TripCircleMember.builder()
                .circle(circle)
                .userId(currentUserId)
                .role(TripCircleMemberRole.MEMBER)
                .build();
        tripCircleMemberRepository.save(member);
    }

    @Override
    public void leaveCircle(String circleId, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        tripCircleMemberRepository.findByCircleAndUserIdAndLeftAtIsNull(circle, currentUserId)
                .ifPresent(member -> {
                    member.setLeftAt(OffsetDateTime.now());
                    tripCircleMemberRepository.save(member);
                });
    }

    // ===================== Posts / Feed =====================

    @Override
    public CirclePostResponse createTodayPlanPost(String circleId, TodayPlanRequest request, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        ensureMember(circle, currentUserId);

        if (enableDedup) {
            enforceActionDedup(currentUserId, circle.getId(), CircleActionType.TODAY_PLAN);
        }

        enforcePostRateLimit(currentUserId);

        String content = buildTodayPlanContent(request.getPlanType(), request.getMessage());

        CirclePost post = CirclePost.builder()
                .circle(circle)
                .authorUserId(currentUserId)
                .postType(PostType.TODAY_PLAN)
                .content(content)
                .status(CirclePostStatus.ACTIVE)
                .build();

        post = circlePostRepository.save(post);
        return toPostResponse(post);
    }

    @Override
    public CirclePostResponse createCheckinPost(String circleId, CheckinRequest request, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        ensureMember(circle, currentUserId);

        if (enableDedup) {
            enforceActionDedup(currentUserId, circle.getId(), CircleActionType.CHECKIN_SAFE);
        }

        enforcePostRateLimit(currentUserId);

        String message = (request.getMessage() == null || request.getMessage().isBlank())
                ? "Reached safely"
                : request.getMessage().trim();

        CirclePost post = CirclePost.builder()
                .circle(circle)
                .authorUserId(currentUserId)
                .postType(PostType.CHECKIN_SAFE)
                .content(message)
                .status(CirclePostStatus.ACTIVE)
                .build();

        post = circlePostRepository.save(post);
        return toPostResponse(post);
    }

    @Override
    public CirclePostResponse createPost(String circleId, CirclePostCreateRequest request, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        ensureMember(circle, currentUserId);
        enforcePostRateLimit(currentUserId);

        CirclePost post = CirclePost.builder()
                .circle(circle)
                .authorUserId(currentUserId)
                .postType(request.getPostType())
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .geoLat(request.getGeoLat())
                .geoLng(request.getGeoLng())
                .status(CirclePostStatus.ACTIVE)
                .build();

        post = circlePostRepository.save(post);
        return toPostResponse(post);
    }

    @Override
    public Page<CirclePostResponse> getCircleFeed(String circleId, String currentUserId, Pageable pageable) {
        TripCircle circle = getCircleOrThrow(circleId);
        ensureMember(circle, currentUserId);
        Page<CirclePost> page = circlePostRepository
                .findByCircleAndStatusOrderByCreatedAtDesc(circle, CirclePostStatus.ACTIVE, pageable);
        return page.map(this::toPostResponse);
    }

    @Override
    public void deletePost(String circleId, String postId, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        CirclePost post = circlePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!post.getCircle().getId().equals(circle.getId())) {
            throw new IllegalArgumentException("Post does not belong to this circle");
        }

        boolean isOwner = isOwner(circle, currentUserId);
        if (!isOwner && !currentUserId.equals(post.getAuthorUserId())) {
            throw new IllegalArgumentException("Only author or circle owner can delete this post");
        }

        post.setStatus(CirclePostStatus.DELETED);
        post.setDeletedAt(OffsetDateTime.now());
        circlePostRepository.save(post);
    }

    // ===================== Polls =====================

    @Override
    public CirclePollResponse createPoll(String circleId, CirclePollRequest request, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        ensureMember(circle, currentUserId);
        enforcePollRateLimit(currentUserId);

        CirclePoll poll = CirclePoll.builder()
                .circle(circle)
                .createdByUserId(currentUserId)
                .question(request.getQuestion())
                .closesAt(request.getClosesAt())
                .status(PollStatus.OPEN)
                .build();
        poll = circlePollRepository.save(poll);

        int order = 0;
        for (String optionText : request.getOptions()) {
            CirclePollOption option = CirclePollOption.builder()
                    .poll(poll)
                    .optionText(optionText)
                    .sortOrder(order++)
                    .build();
            circlePollOptionRepository.save(option);
        }

        return buildPollResponse(poll, currentUserId);
    }

    @Override
    public CirclePollResponse getPoll(String pollId, String currentUserId) {
        CirclePoll poll = circlePollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));
        ensureMember(poll.getCircle(), currentUserId);
        return buildPollResponse(poll, currentUserId);
    }

    @Override
    public List<CirclePollResponse> getPollsByCircle(String circleId, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        ensureMember(circle, currentUserId);
        
        // Get all polls for this circle, ordered by creation date (newest first)
        List<CirclePoll> polls = circlePollRepository.findByCircleOrderByCreatedAtDesc(circle);
        
        List<CirclePollResponse> responses = new ArrayList<>();
        for (CirclePoll poll : polls) {
            responses.add(buildPollResponse(poll, currentUserId));
        }
        return responses;
    }

    @Override
    public CirclePollResponse votePoll(String pollId, String optionId, String currentUserId) {
        CirclePoll poll = circlePollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));
        ensureMember(poll.getCircle(), currentUserId);

        if (poll.getStatus() != PollStatus.OPEN) {
            throw new IllegalArgumentException("Poll is not open");
        }

        if (poll.getClosesAt() != null && poll.getClosesAt().isBefore(OffsetDateTime.now())) {
            poll.setStatus(PollStatus.CLOSED);
            circlePollRepository.save(poll);
            throw new IllegalArgumentException("Poll is closed");
        }

        CirclePollOption option = circlePollOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Option not found"));
        if (!option.getPoll().getId().equals(poll.getId())) {
            throw new IllegalArgumentException("Option does not belong to this poll");
        }

        CirclePollVote vote = circlePollVoteRepository.findByPollAndUserId(poll, currentUserId)
                .orElseGet(() -> CirclePollVote.builder()
                        .poll(poll)
                        .userId(currentUserId)
                        .build());
        vote.setOption(option);
        vote.setVotedAt(OffsetDateTime.now());
        circlePollVoteRepository.save(vote);

        return buildPollResponse(poll, currentUserId);
    }

    // ===================== Booking Locks & Attribution =====================

    @Override
    public CircleBookingLocksResponse getBookingLocks(String circleId, String currentUserId) {
        TripCircle circle = getCircleOrThrow(circleId);
        ensureMember(circle, currentUserId);

        List<BookingLockResponse.BookingAction> actions = new ArrayList<>();
        actions.add(BookingLockResponse.BookingAction.builder()
                .type("CREATE_BOOKING")
                .label("Book your trip")
                .url("/bookings?circleId=" + circle.getId())
                .build());
        actions.add(BookingLockResponse.BookingAction.builder()
                .type("VIEW_DEALS")
                .label("View deals for this destination")
                .url("/deals?destinationId=" + circle.getDestinationId())
                .build());

        String trackingToken = buildTrackingToken(circleId, currentUserId);

        return CircleBookingLocksResponse.builder()
                .circleId(circleId)
                .actions(actions)
                .trackingToken(trackingToken)
                .build();
    }

    @Override
    public void recordBookingClick(CircleBookingClickRequest request, String currentUserId) {
        TripCircle circle = getCircleOrThrow(request.getCircleId());
        ensureMember(circle, currentUserId);

        CircleLastClick lastClick = circleLastClickRepository.findByUserId(currentUserId)
                .orElseGet(() -> CircleLastClick.builder().userId(currentUserId).build());
        lastClick.setCircleId(request.getCircleId());
        lastClick.setPostId(request.getPostId());
        lastClick.setRefUserId(request.getRefUserId());
        lastClick.setClickedAt(OffsetDateTime.now());
        circleLastClickRepository.save(lastClick);
    }

    @Override
    public BookingAttributionResponse recordBookingAttribution(BookingAttributionRequest request) {
        Optional<BookingAttribution> existingOpt = bookingAttributionRepository.findByBookingId(request.getBookingId());
        if (existingOpt.isPresent()) {
            return toAttributionResponse(existingOpt.get(), false);
        }

        String circleId = request.getCircleId();
        String postId = request.getPostId();
        String refUserId = request.getRefUserId();

        if (request.getTrackingToken() != null && !request.getTrackingToken().isBlank()) {
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(request.getTrackingToken()), StandardCharsets.UTF_8);
                String[] parts = decoded.split(":", -1);
                if (parts.length >= 2) {
                    circleId = parts[0];
                    if (refUserId == null || refUserId.isBlank()) {
                        refUserId = parts[1];
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to decode tracking token for booking {}: {}", request.getBookingId(), e.getMessage());
            }
        }

        if ((circleId == null || circleId.isBlank()) || (refUserId == null || refUserId.isBlank())) {
            Optional<CircleLastClick> lastClickOpt = circleLastClickRepository.findByUserId(request.getBookerUserId());
            if (lastClickOpt.isPresent()) {
                CircleLastClick lastClick = lastClickOpt.get();
                if (lastClick.getClickedAt().isAfter(OffsetDateTime.now().minusDays(bookingAttributionWindowDays))) {
                    if (circleId == null || circleId.isBlank()) {
                        circleId = lastClick.getCircleId();
                    }
                    if (refUserId == null || refUserId.isBlank()) {
                        refUserId = lastClick.getRefUserId();
                    }
                    if (postId == null || postId.isBlank()) {
                        postId = lastClick.getPostId();
                    }
                }
            }
        }

        boolean eligible = true;
        if (refUserId != null && !refUserId.isBlank() && refUserId.equals(request.getBookerUserId())) {
            eligible = false;
        }

        BookingAttribution attribution = BookingAttribution.builder()
                .bookingId(request.getBookingId())
                .bookerUserId(request.getBookerUserId())
                .circleId(circleId)
                .postId(postId)
                .refUserId(refUserId)
                .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .eligible(eligible)
                .build();

        attribution = bookingAttributionRepository.save(attribution);
        return toAttributionResponse(attribution, true);
    }

    // ===================== Helpers =====================

    private TripCircle getCircleOrThrow(String circleId) {
        return tripCircleRepository.findById(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Trip circle not found"));
    }

    private void ensureMember(TripCircle circle, String userId) {
        boolean member = tripCircleMemberRepository.existsByCircleAndUserIdAndLeftAtIsNull(circle, userId);
        if (!member) {
            throw new IllegalArgumentException("You are not a member of this circle");
        }
    }

    private boolean isOwner(TripCircle circle, String userId) {
        return tripCircleMemberRepository.findByCircleAndRoleAndLeftAtIsNull(circle, TripCircleMemberRole.OWNER)
                .stream()
                .anyMatch(m -> m.getUserId().equals(userId));
    }

    private void validateCircleDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end date are required");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (start.plusMonths(12).isBefore(end)) {
            throw new IllegalArgumentException("Circle duration is too long");
        }
    }

    private void enforcePostRateLimit(String userId) {
        if (maxPostsPerHour <= 0) {
            return;
        }
        OffsetDateTime windowStart = OffsetDateTime.now().withMinute(0).withSecond(0).withNano(0);
        String bucket = RateLimitBucket.CIRCLE_POST.name();
        RateLimitCounter counter = rateLimitCounterRepository
                .findByUserIdAndBucketAndWindowStart(userId, bucket, windowStart)
                .orElseGet(() -> RateLimitCounter.builder()
                        .userId(userId)
                        .bucket(bucket)
                        .windowStart(windowStart)
                        .count(0)
                        .build());
        if (counter.getId() != null && counter.getCount() >= maxPostsPerHour) {
            throw new IllegalArgumentException("Rate limit exceeded for posts");
        }
        counter.setCount(counter.getCount() + 1);
        rateLimitCounterRepository.save(counter);
    }

    private void enforceCircleRateLimit(String userId) {
        if (maxCirclesPerDay <= 0) {
            return;
        }
        OffsetDateTime windowStart = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        String bucket = RateLimitBucket.CIRCLE_CREATE.name();
        RateLimitCounter counter = rateLimitCounterRepository
                .findByUserIdAndBucketAndWindowStart(userId, bucket, windowStart)
                .orElseGet(() -> RateLimitCounter.builder()
                        .userId(userId)
                        .bucket(bucket)
                        .windowStart(windowStart)
                        .count(0)
                        .build());
        if (counter.getId() != null && counter.getCount() >= maxCirclesPerDay) {
            throw new IllegalArgumentException("Rate limit exceeded for creating circles");
        }
        counter.setCount(counter.getCount() + 1);
        rateLimitCounterRepository.save(counter);
    }

    private void enforcePollRateLimit(String userId) {
        if (maxPollsPerDay <= 0) {
            return;
        }
        OffsetDateTime windowStart = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        String bucket = RateLimitBucket.CIRCLE_POLL.name();
        RateLimitCounter counter = rateLimitCounterRepository
                .findByUserIdAndBucketAndWindowStart(userId, bucket, windowStart)
                .orElseGet(() -> RateLimitCounter.builder()
                        .userId(userId)
                        .bucket(bucket)
                        .windowStart(windowStart)
                        .count(0)
                        .build());
        if (counter.getId() != null && counter.getCount() >= maxPollsPerDay) {
            throw new IllegalArgumentException("Rate limit exceeded for creating polls");
        }
        counter.setCount(counter.getCount() + 1);
        rateLimitCounterRepository.save(counter);
    }

    private void enforceActionDedup(String userId, String circleId, CircleActionType actionType) {
        LocalDate today = LocalDate.now();
        Optional<UserActionDedup> existing = userActionDedupRepository
                .findByUserIdAndCircleIdAndActionTypeAndActionDate(userId, circleId, actionType.name(), today);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Action already performed today");
        }
        UserActionDedup dedup = UserActionDedup.builder()
                .userId(userId)
                .circleId(circleId)
                .actionType(actionType.name())
                .actionDate(today)
                .build();
        userActionDedupRepository.save(dedup);
    }

    private String buildTodayPlanContent(PlanType type, String message) {
        String base;
        if (type == null) {
            base = "Today's plan";
        } else {
            switch (type) {
                case SIGHTSEEING -> base = "Sightseeing today";
                case ADVENTURE -> base = "Adventure day";
                case FOOD -> base = "Food exploration";
                case SHOPPING -> base = "Shopping day";
                case REST -> base = "Rest day";
                case TRAVEL_DAY -> base = "Travel day";
                default -> base = "Today's plan";
            }
        }
        if (message != null && !message.isBlank()) {
            return base + ": " + message.trim();
        }
        return base;
    }

    private TripCircleResponse toResponse(TripCircle circle, String currentUserId) {
        List<TripCircleMember> members = tripCircleMemberRepository.findByCircleAndLeftAtIsNull(circle);
        boolean joined = false;
        String role = null;
        for (TripCircleMember member : members) {
            if (member.getUserId().equals(currentUserId)) {
                joined = true;
                role = member.getRole().name();
                break;
            }
        }
        return TripCircleResponse.builder()
                .id(circle.getId())
                .destinationId(circle.getDestinationId())
                .title(circle.getTitle())
                .startDate(circle.getStartDate())
                .endDate(circle.getEndDate())
                .createdByUserId(circle.getCreatedByUserId())
                .visibility(circle.getVisibility())
                .status(circle.getStatus())
                .createdAt(circle.getCreatedAt())
                .updatedAt(circle.getUpdatedAt())
                .joined(joined)
                .memberRole(role)
                .memberCount(members.size())
                .build();
    }

    private CirclePostResponse toPostResponse(CirclePost post) {
        Optional<User> userOpt = userRepository.findById(post.getAuthorUserId());
        String firstName = null;
        String lastName = null;
        String avatar = null;
        boolean verified = false;

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            User.PrivacyPreferences privacy = user.getPreferences() != null
                    ? user.getPreferences().getPrivacy()
                    : null;

            boolean isProfileVisible = privacy != null && Boolean.TRUE.equals(privacy.getProfileVisible());

            if (isProfileVisible) {
                // Visible profile: show real name/avatar (still no email/phone/etc.)
                firstName = user.getFirstName();
                lastName = user.getLastName();
                avatar = user.getAvatarUrl();
            } else {
                // Private profile: anonymize author identity in feed
                firstName = "Traveler";
                lastName = null;
                avatar = null;
            }

            verified = Boolean.TRUE.equals(user.getIsVerified());
        }

        return CirclePostResponse.builder()
                .id(post.getId())
                .circleId(post.getCircle().getId())
                .authorUserId(post.getAuthorUserId())
                .authorFirstName(firstName)
                .authorLastName(lastName)
                .authorAvatarUrl(avatar)
                .authorVerified(verified)
                .postType(post.getPostType())
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .geoLat(post.getGeoLat())
                .geoLng(post.getGeoLng())
                .status(post.getStatus())
                .createdAt(post.getCreatedAt())
                .build();
    }

    private CirclePollResponse buildPollResponse(CirclePoll poll, String currentUserId) {
        List<CirclePollOption> options = circlePollOptionRepository.findByPollOrderBySortOrderAsc(poll);
        Optional<CirclePollVote> myVote = circlePollVoteRepository.findByPollAndUserId(poll, currentUserId);
        String myOptionId = myVote.map(v -> v.getOption().getId()).orElse(null);

        List<CirclePollOptionResponse> optionResponses = new ArrayList<>();
        for (CirclePollOption option : options) {
            long count = circlePollVoteRepository.countByPollAndOption(poll, option);
            optionResponses.add(CirclePollOptionResponse.builder()
                    .id(option.getId())
                    .text(option.getOptionText())
                    .voteCount(count)
                    .selectedByCurrentUser(option.getId().equals(myOptionId))
                    .build());
        }

        return CirclePollResponse.builder()
                .id(poll.getId())
                .circleId(poll.getCircle().getId())
                .question(poll.getQuestion())
                .status(poll.getStatus())
                .createdAt(poll.getCreatedAt())
                .closesAt(poll.getClosesAt())
                .options(optionResponses)
                .build();
    }

    private String buildDefaultTitle(TripCircleCreateRequest request) {
        return "Trip to " + request.getDestinationId() + " (" + request.getStartDate() + " - " + request.getEndDate() + ")";
    }

    private String buildTrackingToken(String circleId, String currentUserId) {
        String raw = circleId + ":" + currentUserId + ":" + OffsetDateTime.now();
        return Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private BookingAttributionResponse toAttributionResponse(BookingAttribution attribution, boolean created) {
        return BookingAttributionResponse.builder()
                .bookingId(attribution.getBookingId())
                .bookerUserId(attribution.getBookerUserId())
                .circleId(attribution.getCircleId())
                .postId(attribution.getPostId())
                .refUserId(attribution.getRefUserId())
                .eligible(attribution.isEligible())
                .amount(attribution.getAmount())
                .created(created)
                .build();
    }
}
