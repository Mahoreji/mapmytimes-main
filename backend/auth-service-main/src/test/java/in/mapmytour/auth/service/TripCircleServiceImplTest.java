package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.user.BookingAttributionRequest;
import in.mapmytour.auth.dto.user.BookingAttributionResponse;
import in.mapmytour.auth.dto.user.TodayPlanRequest;
import in.mapmytour.auth.entity.PlanType;
import in.mapmytour.auth.service.impl.TripCircleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lightweight unit tests for TripCircleServiceImpl core behaviours.
 *
 * These do not cover all branches but validate key business rules
 * like daily dedup, attribution self-eligibility and basic flows.
 */
public class TripCircleServiceImplTest {

        @Mock
        private in.mapmytour.auth.repository.TripCircleRepository tripCircleRepository;
        @Mock
        private in.mapmytour.auth.repository.TripCircleMemberRepository tripCircleMemberRepository;
        @Mock
        private in.mapmytour.auth.repository.CirclePostRepository circlePostRepository;
        @Mock
        private in.mapmytour.auth.repository.CirclePollRepository circlePollRepository;
        @Mock
        private in.mapmytour.auth.repository.CirclePollOptionRepository circlePollOptionRepository;
        @Mock
        private in.mapmytour.auth.repository.CirclePollVoteRepository circlePollVoteRepository;
        @Mock
        private in.mapmytour.auth.repository.BookingAttributionRepository bookingAttributionRepository;
        @Mock
        private in.mapmytour.auth.repository.UserActionDedupRepository userActionDedupRepository;
        @Mock
        private in.mapmytour.auth.repository.RateLimitCounterRepository rateLimitCounterRepository;
        @Mock
        private in.mapmytour.auth.repository.CircleLastClickRepository circleLastClickRepository;
        @Mock
        private in.mapmytour.auth.repository.UserRepository userRepository;

        @InjectMocks
        private TripCircleServiceImpl tripCircleService;

        @BeforeEach
        void setup() {
                MockitoAnnotations.openMocks(this);
                org.springframework.test.util.ReflectionTestUtils.setField(tripCircleService, "enableDedup", true);
        }

        @Test
        void todayPlan_dedupSameDay_throws() {
                // given
                String circleId = "circle-1";
                String userId = "user-1";
                TodayPlanRequest req = new TodayPlanRequest();
                req.setPlanType(PlanType.SIGHTSEEING);

                in.mapmytour.auth.entity.TripCircle circle = in.mapmytour.auth.entity.TripCircle.builder()
                                .id(circleId)
                                .destinationId("DEST")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(1))
                                .createdByUserId(userId)
                                .build();

                when(tripCircleRepository.findById(circleId)).thenReturn(java.util.Optional.of(circle));
                when(tripCircleMemberRepository.existsByCircleAndUserIdAndLeftAtIsNull(circle, userId))
                                .thenReturn(true);
                when(userActionDedupRepository.findByUserIdAndCircleIdAndActionTypeAndActionDate(any(), any(), any(),
                                any()))
                                .thenReturn(java.util.Optional.of(new in.mapmytour.auth.entity.UserActionDedup()));
                in.mapmytour.auth.entity.User mockUser = new in.mapmytour.auth.entity.User();
                mockUser.setRoles(java.util.Set.of());
                when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));

                // then
                assertThrows(IllegalArgumentException.class,
                                () -> tripCircleService.createTodayPlanPost(circleId, req, userId));
        }

        @Test
        void bookingAttribution_selfBooking_notEligible() {
                BookingAttributionRequest request = new BookingAttributionRequest();
                request.setBookingId("B1");
                request.setBookerUserId("U1");
                request.setCircleId("C1");
                request.setPostId("P1");
                request.setRefUserId("U1"); // self
                request.setAmount(BigDecimal.TEN);

                when(bookingAttributionRepository.findByBookingId("B1"))
                                .thenReturn(java.util.Optional.empty());
                when(bookingAttributionRepository.save(any()))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                BookingAttributionResponse resp = tripCircleService.recordBookingAttribution(request);
                assertNotNull(resp);
                assertFalse(resp.isEligible(), "self booking should not be eligible");
                assertEquals("B1", resp.getBookingId());
        }
}
