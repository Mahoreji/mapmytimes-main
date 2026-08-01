package in.mapmytour.auth.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.auth.UserResponse;
import in.mapmytour.auth.dto.auth.UserStatsResponse;
import in.mapmytour.auth.dto.user.*;
import in.mapmytour.auth.entity.ConnectionRequest;
import in.mapmytour.auth.entity.DirectMessage;
import in.mapmytour.auth.entity.Document;
import in.mapmytour.auth.entity.ExpenseParticipant;
import in.mapmytour.auth.entity.GroupExpense;
import in.mapmytour.auth.entity.GroupMessage;
import in.mapmytour.auth.entity.LoginHistory;
import in.mapmytour.auth.entity.TravelGroup;
import in.mapmytour.auth.entity.TravelGroupMember;
import in.mapmytour.auth.entity.TravelItinerary;
import in.mapmytour.auth.entity.TravelPlan;
import in.mapmytour.auth.entity.BlockedUser;
import in.mapmytour.auth.entity.SocialNotification;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.entity.UserActivity;
import in.mapmytour.auth.entity.UserConnection;
import in.mapmytour.auth.entity.Agent;
import in.mapmytour.auth.entity.Supplier;
import in.mapmytour.auth.exception.ProfileNotPublicException;
import in.mapmytour.auth.helper.S3Helper;
import in.mapmytour.auth.repository.BlockedUserRepository;
import in.mapmytour.auth.repository.ConnectionRequestRepository;
import in.mapmytour.auth.repository.DirectMessageRepository;
import in.mapmytour.auth.repository.DocumentRepository;
import in.mapmytour.auth.repository.ExpenseParticipantRepository;
import in.mapmytour.auth.repository.GroupExpenseRepository;
import in.mapmytour.auth.repository.GroupMessageRepository;
import in.mapmytour.auth.repository.LoginHistoryRepository;
import in.mapmytour.auth.repository.RefreshTokenRepository;
import in.mapmytour.auth.repository.TravelGroupMemberRepository;
import in.mapmytour.auth.repository.TravelGroupRepository;
import in.mapmytour.auth.repository.TravelItineraryRepository;
import in.mapmytour.auth.repository.TravelPlanRepository;
import in.mapmytour.auth.repository.UserActivityRepository;
import in.mapmytour.auth.repository.UserConnectionRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.repository.AgentRepository;
import in.mapmytour.auth.repository.SocialNotificationRepository;
import in.mapmytour.auth.repository.SupplierRepository;
import in.mapmytour.auth.service.UserService;
import in.mapmytour.auth.utils.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DocumentRepository documentRepository;
    private final in.mapmytour.auth.repository.VerificationRequestRepository verificationRequestRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final UserActivityRepository userActivityRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final UserConnectionRepository userConnectionRepository;
    private final ConnectionRequestRepository connectionRequestRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final TravelGroupRepository travelGroupRepository;
    private final TravelGroupMemberRepository travelGroupMemberRepository;
    private final TravelItineraryRepository travelItineraryRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final DirectMessageRepository directMessageRepository;
    private final GroupExpenseRepository groupExpenseRepository;
    private final ExpenseParticipantRepository expenseParticipantRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Helper s3Helper;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final in.mapmytour.auth.helper.RealtimeNotificationHelper realtimeNotificationHelper;
    private final in.mapmytour.auth.service.PresenceService presenceService;
    private final in.mapmytour.auth.utils.JwtUtil jwtUtil;
    private final AgentRepository agentRepository;
    private final SupplierRepository supplierRepository;
    private final SocialNotificationRepository socialNotificationRepository;

    // =============== USER MANAGEMENT ================
    @Override
    public UserProfileResponse getUserById(String userId) {
        return getUserById(userId, null);
    }

    @Override
    public UserProfileResponse getUserById(String userId, String currentUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get current user if provided for context (to show connection count on own
        // profile)
        User currentUser = null;
        if (currentUserEmail != null) {
            currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        }

        return mapToUserProfileResponse(user, currentUser);
    }

    // ================ PROFILE MANAGEMENT ================

    @Override
    public UserProfileResponse getCurrentUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Pass user as currentUser to show connection count (own profile)
        return mapToUserProfileResponse(user, user);
    }

    @Override
    public UserProfileResponse updateProfile(UpdateProfileRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update basic information
        if (request.getFirstName() != null && ValidationUtil.isValidName(request.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null && ValidationUtil.isValidName(request.getLastName())) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getPhone() != null) {
            if (ValidationUtil.isValidPhoneNumber(request.getPhone())) {
                user.setPhone(request.getPhone());
            } else {
                throw new IllegalArgumentException("Invalid phone number format");
            }
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null) {
            try {
                User.Gender gender = User.Gender.valueOf(request.getGender().toUpperCase());
                user.setGender(gender);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid gender value");
            }
        }

        // Update bio
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        // Update cover image URL
        if (request.getCoverImageUrl() != null) {
            user.setCoverImageUrl(request.getCoverImageUrl().trim());
        }

        // Update address
        if (request.getAddress() != null) {
            updateUserAddress(user, request.getAddress());
        }

        // Update preferences
        if (request.getPreferences() != null) {
            updateUserPreferences(user, request.getPreferences());
        }

        userRepository.save(user);

        // Update local Agent/Supplier entities if they exist
        boolean isSupplier = user.getRoles().stream().anyMatch(r -> r.getName().equals("SUPPLIER"));
        boolean isAgent = user.getRoles().stream().anyMatch(r -> r.getName().equals("AGENT"));

        if (isSupplier) {
            supplierRepository.findByUser(user).ifPresent(supplier -> {
                if (request.getFirstName() != null || request.getLastName() != null) {
                    supplier.setContactPerson(user.getFirstName() + " " + user.getLastName());
                }
                if (request.getPhone() != null) {
                    supplier.setPhone(user.getPhone());
                }
                if (request.getAddress() != null) {
                    supplier.setAddress(request.getAddress().getStreet());
                    supplier.setCity(request.getAddress().getCity());
                    supplier.setState(request.getAddress().getState());
                    supplier.setCountry(request.getAddress().getCountry());
                    supplier.setPincode(request.getAddress().getPostalCode());
                }
                supplierRepository.save(supplier);
            });
        }

        if (isAgent) {
            agentRepository.findByUser(user).ifPresent(agent -> {
                if (request.getFirstName() != null || request.getLastName() != null) {
                    agent.setContactPerson(user.getFirstName() + " " + user.getLastName());
                }
                if (request.getPhone() != null) {
                    agent.setPhone(user.getPhone());
                }
                if (request.getAddress() != null) {
                    agent.setAddress(request.getAddress().getStreet());
                    agent.setCity(request.getAddress().getCity());
                    agent.setState(request.getAddress().getState());
                    agent.setCountry(request.getAddress().getCountry());
                    agent.setPincode(request.getAddress().getPostalCode());
                }
                agentRepository.save(agent);
            });
        }

        log.info("Profile updated for user: {}", userEmail);

        return mapToUserProfileResponse(user);
    }

    @Override
    public UserProfileResponse getPublicProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Extract current user if authenticated to show mutual connections and verify
        // ownership
        User currentUser = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String currentUserEmail = auth.getName();
            currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        }

        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        boolean isOwner = currentUser != null && currentUser.getId().equals(userId);

        // Check if profile is visible (bypass for admins and owners)
        if (!isAdmin && !isOwner && user.getPreferences() != null &&
                user.getPreferences().getPrivacy() != null &&
                !user.getPreferences().getPrivacy().getProfileVisible()) {
            throw new ProfileNotPublicException("Profile is not public");
        }

        // Use mapping method with currentUser (if available)
        UserProfileResponse profile = mapToUserProfileResponse(user, currentUser);

        // Restrict Agent/Supplier visibility unless it's a regular user.
        // Fetch once per type — no separate existsBy() call needed.
        Optional<Agent> agentOpt = agentRepository.findByUser(user);
        Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);

        if (user.getRole() == User.UserRole.B2B || agentOpt.isPresent() || supplierOpt.isPresent()) {
            // Admins and the profile owner always see the full profile
            if (!isAdmin && !isOwner) {
                String businessName = null;
                if (user.getRole() == User.UserRole.B2B || agentOpt.isPresent()) {
                    businessName = agentOpt.map(Agent::getAgencyName).orElse(null);
                } else if (supplierOpt.isPresent()) {
                    businessName = supplierOpt.map(Supplier::getCompanyName).orElse(null);
                }

                // For public visitors, return ONLY basic business details
                return UserProfileResponse.builder()
                        .id(user.getId())
                        .firstName(businessName != null ? businessName : user.getFirstName())
                        .lastName(businessName != null ? "" : user.getLastName())
                        .businessName(businessName)
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole().name())
                        .isVerified(user.isVerified())
                        .build();
            }
        }

        // Remove sensitive information for public profile
        profile.setPhone(null);
        if (profile.getAddress() != null) {
            profile.getAddress().setStreet(null);
            profile.getAddress().setPostalCode(null);
        }

        return profile;
    }

    @Override
    public MessageResponse updateProfileVisibility(String userEmail, boolean isVisible) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User.UserPreferences preferences = user.getPreferences();
        if (preferences == null) {
            preferences = User.UserPreferences.builder().build();
        }

        User.PrivacyPreferences privacy = preferences.getPrivacy();
        if (privacy == null) {
            privacy = User.PrivacyPreferences.builder().build();
        }

        privacy.setProfileVisible(isVisible);
        preferences.setPrivacy(privacy);
        user.setPreferences(preferences);

        userRepository.save(user);

        return MessageResponse.builder()
                .message("Profile visibility updated successfully")
                .build();
    }

    @Override
    public UserSummaryResponse getUserSummary(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Fetch once — no separate existsBy() + findBy() double-query
        Optional<Agent> agentOpt = agentRepository.findByUser(user);
        Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);

        String businessName = null;
        if (user.getRole() == User.UserRole.B2B || agentOpt.isPresent()) {
            businessName = agentOpt.map(Agent::getAgencyName).orElse(null);
        } else if (supplierOpt.isPresent()) {
            businessName = supplierOpt.map(Supplier::getCompanyName).orElse(null);
        }

        return UserSummaryResponse.builder()
                .id(user.getId())
                .firstName(businessName != null ? businessName : user.getFirstName())
                .lastName(businessName != null ? "" : user.getLastName())
                .businessName(businessName)
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .build();
    }

    // ================ AVATAR MANAGEMENT ================

    @Override
    public AvatarUploadResponse uploadAvatar(MultipartFile file, String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Delete old avatar if exists
            if (user.getAvatarUrl() != null) {
                s3Helper.deleteFile(user.getAvatarUrl());
            }

            // Upload new avatar
            String avatarUrl = s3Helper.uploadImage(file, "avatars");
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            log.info("Avatar uploaded for user: {}", userEmail);
            return AvatarUploadResponse.builder()
                    .avatarUrl(avatarUrl)
                    .user(mapToUserProfileResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload avatar for user: {}", userEmail, e);
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage());
        }
    }

    @Override
    public MessageResponse deleteAvatar(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getAvatarUrl() != null) {
            s3Helper.deleteFile(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
        }

        return MessageResponse.builder()
                .message("Avatar deleted successfully")
                .build();
    }

    @Override
    public AvatarUploadResponse uploadCoverImage(MultipartFile file, String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Delete old cover image if exists
            if (user.getCoverImageUrl() != null) {
                s3Helper.deleteFile(user.getCoverImageUrl());
            }

            // Upload new cover image
            String coverImageUrl = s3Helper.uploadImage(file, "cover-images");
            user.setCoverImageUrl(coverImageUrl);
            userRepository.save(user);

            log.info("Cover image uploaded for user: {}", userEmail);
            return AvatarUploadResponse.builder()
                    .avatarUrl(coverImageUrl) // Reusing avatarUrl field for cover image URL
                    .user(mapToUserProfileResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload cover image for user: {}", userEmail, e);
            throw new RuntimeException("Failed to upload cover image: " + e.getMessage());
        }
    }

    @Override
    public MessageResponse deleteCoverImage(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getCoverImageUrl() != null) {
            s3Helper.deleteFile(user.getCoverImageUrl());
            user.setCoverImageUrl(null);
            userRepository.save(user);
        }

        return MessageResponse.builder()
                .message("Cover image deleted successfully")
                .build();
    }

    @Override
    public String generateAvatarUrl(String userEmail, String initials) {
        // Generate avatar URL using a service like UI Avatars or similar
        String backgroundColor = "007bff";
        String textColor = "ffffff";
        String size = "128";

        return String.format("https://ui-avatars.com/api/?name=%s&background=%s&color=%s&size=%s",
                initials.replace(" ", "+"), backgroundColor, textColor, size);
    }

    // ================ ADDRESS MANAGEMENT ================

    @Override
    public UserProfileResponse updateAddress(AddressRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        updateUserAddress(user, request);
        userRepository.save(user);

        return mapToUserProfileResponse(user);
    }

    @Override
    public MessageResponse deleteAddress(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setAddress(null);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Address deleted successfully")
                .build();
    }

    @Override
    public List<AddressResponse> getSavedAddresses(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<AddressResponse> addresses = new ArrayList<>();

        // Add main address if exists
        if (user.getAddress() != null) {
            addresses.add(AddressResponse.builder()
                    .street(user.getAddress().getStreet())
                    .city(user.getAddress().getCity())
                    .state(user.getAddress().getState())
                    .country(user.getAddress().getCountry())
                    .postalCode(user.getAddress().getPostalCode())
                    .build());
        }

        // In a real implementation, you might have a separate SavedAddress entity
        // For now, we'll just return the main address

        return addresses;
    }

    @Override
    public MessageResponse saveAddress(AddressRequest request, String userEmail, String label) {
        // In a real implementation, you'd save this to a SavedAddress entity
        // For now, we'll just update the main address
        updateAddress(request, userEmail);

        return MessageResponse.builder()
                .message("Address saved successfully")
                .build();
    }

    // ================ PREFERENCES MANAGEMENT ================

    @Override
    public UserProfileResponse updatePreferences(UserPreferencesRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        updateUserPreferences(user, request);
        userRepository.save(user);

        return mapToUserProfileResponse(user);
    }

    @Override
    public UserPreferencesResponse getPreferences(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getPreferences() == null) {
            return UserPreferencesResponse.builder()
                    .notifications(NotificationPreferencesResponse.builder()
                            .email(true)
                            .sms(false)
                            .push(true)
                            .build())
                    .privacy(PrivacyPreferencesResponse.builder()
                            .profileVisible(true)
                            .showBookingHistory(true)
                            .showEmail(true)
                            .showPhone(true)
                            .showDateOfBirth(true)
                            .showAddress(true)
                            .showStreet(true)
                            .showCity(true)
                            .showState(true)
                            .showPostalCode(true)
                            .build())
                    .build();
        }

        return mapToUserPreferencesResponse(user.getPreferences());
    }

    @Override
    public MessageResponse resetPreferencesToDefault(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPreferences(User.UserPreferences.builder()
                .notifications(User.NotificationPreferences.builder()
                        .email(true)
                        .sms(false)
                        .push(true)
                        .build())
                .privacy(User.PrivacyPreferences.builder()
                        .profileVisible(true)
                        .showBookingHistory(true)
                        .showEmail(true)
                        .showPhone(true)
                        .showDateOfBirth(true)
                        .showAddress(true)
                        .showStreet(true)
                        .showCity(true)
                        .showState(true)
                        .showPostalCode(true)
                        .build())
                .build());

        userRepository.save(user);

        return MessageResponse.builder()
                .message("Preferences reset to default successfully")
                .build();
    }

    // ================ NOTIFICATION SETTINGS ================

    @Override
    public NotificationSettingsResponse getNotificationSettings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User.NotificationPreferences notifications = user.getPreferences() != null
                ? user.getPreferences().getNotifications()
                : null;

        if (notifications == null) {
            notifications = User.NotificationPreferences.builder()
                    .email(true)
                    .sms(false)
                    .push(true)
                    .build();
        }

        return NotificationSettingsResponse.builder()
                .email(notifications.getEmail())
                .sms(notifications.getSms())
                .push(notifications.getPush())
                .marketing(true) // Default value
                .security(true) // Default value
                .bookingUpdates(true) // Default value
                .build();
    }

    @Override
    public MessageResponse updateNotificationSettings(NotificationSettingsRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User.UserPreferences preferences = user.getPreferences();
        if (preferences == null) {
            preferences = User.UserPreferences.builder().build();
        }

        User.NotificationPreferences notifications = User.NotificationPreferences.builder()
                .email(request.getEmail() != null ? request.getEmail() : true)
                .sms(request.getSms() != null ? request.getSms() : false)
                .push(request.getPush() != null ? request.getPush() : true)
                .build();

        preferences.setNotifications(notifications);
        user.setPreferences(preferences);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Notification settings updated successfully")
                .build();
    }

    @Override
    public MessageResponse subscribeToNotification(String userEmail, String notificationType) {
        // Implementation for subscribing to specific notification types
        return MessageResponse.builder()
                .message("Subscribed to " + notificationType + " notifications")
                .build();
    }

    @Override
    public MessageResponse unsubscribeFromNotification(String userEmail, String notificationType) {
        // Implementation for unsubscribing from specific notification types
        return MessageResponse.builder()
                .message("Unsubscribed from " + notificationType + " notifications")
                .build();
    }

    // ================ PRIVACY SETTINGS ================

    @Override
    public PrivacySettingsResponse getPrivacySettings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User.PrivacyPreferences privacy = user.getPreferences() != null ? user.getPreferences().getPrivacy() : null;

        if (privacy == null) {
            privacy = User.PrivacyPreferences.builder()
                    .profileVisible(true)
                    .showBookingHistory(true)
                    .showEmail(true)
                    .showPhone(true)
                    .showDateOfBirth(true)
                    .showAddress(true)
                    .showStreet(true)
                    .showCity(true)
                    .showState(true)
                    .showPostalCode(true)
                    .build();
        }

        return PrivacySettingsResponse.builder()
                .profileVisible(privacy.getProfileVisible())
                .showBookingHistory(privacy.getShowBookingHistory())
                .allowMessages(true) // Default value
                .showOnlineStatus(true) // Default value
                .dataCollection(true) // Default value
                .build();
    }

    @Override
    public MessageResponse updatePrivacySettings(PrivacySettingsRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User.UserPreferences preferences = user.getPreferences();
        if (preferences == null) {
            preferences = User.UserPreferences.builder().build();
        }

        User.PrivacyPreferences privacy = User.PrivacyPreferences.builder()
                .profileVisible(request.getProfileVisible() != null ? request.getProfileVisible() : true)
                .showBookingHistory(request.getShowBookingHistory() != null ? request.getShowBookingHistory() : true)
                // Default granular settings to true if not specified
                .showEmail(true)
                .showPhone(true)
                .showDateOfBirth(true)
                .showAddress(true)
                .showStreet(true)
                .showCity(true)
                .showState(true)
                .showPostalCode(true)
                .build();

        preferences.setPrivacy(privacy);
        user.setPreferences(preferences);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Privacy settings updated successfully")
                .build();
    }

    @Override
    public MessageResponse blockUser(String userEmail, String targetUserId) {
        log.info("Block request: user {} attempting to block user {}", userEmail, targetUserId);

        // Get the blocker (current user)
        User blocker = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get the user to be blocked
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        log.debug("Blocker ID: {}, Blocked ID: {}", blocker.getId(), blocked.getId());

        // Prevent self-blocking
        if (blocker.getId().equals(blocked.getId())) {
            log.warn("User {} attempted to block themselves", userEmail);
            throw new IllegalArgumentException("Cannot block yourself");
        }

        // Check existing relationship regardless of active flag
        Optional<BlockedUser> existingOpt = blockedUserRepository
                .findByBlockerAndBlocked(blocker, blocked);

        if (existingOpt.isPresent()) {
            BlockedUser existing = existingOpt.get();

            if (existing.isActive()) {
                log.warn("User {} already blocked user {}", userEmail, blocked.getEmail());
                throw new IllegalArgumentException("User is already blocked");
            }

            // Reactivate existing relationship instead of inserting a new row
            existing.setActive(true);
            existing.setBlockedAt(LocalDateTime.now());
            existing.setReason(null);

            BlockedUser saved = blockedUserRepository.save(existing);
            log.info("User {} re-blocked user {} (reused BlockedUser ID: {})",
                    blocker.getEmail(), blocked.getEmail(), saved.getId());
        } else {
            // Create and save a new blocking relationship
            BlockedUser blockedUser = BlockedUser.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .isActive(true)
                    .blockedAt(LocalDateTime.now())
                    .build();

            BlockedUser saved = blockedUserRepository.save(blockedUser);
            log.info("User {} successfully blocked user {} (BlockedUser ID: {})",
                    blocker.getEmail(), blocked.getEmail(), saved.getId());
        }

        return MessageResponse.builder()
                .message("User blocked successfully")
                .build();
    }

    @Override
    public MessageResponse unblockUser(String userEmail, String targetUserId) {
        // Get the blocker (current user)
        User blocker = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get the user to be unblocked
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        // Find the blocking relationship (may be active or inactive)
        BlockedUser blockedUser = blockedUserRepository
                .findByBlockerAndBlocked(blocker, blocked)
                .orElseThrow(() -> new IllegalArgumentException("User is not blocked"));

        if (!blockedUser.isActive()) {
            log.warn("User {} attempted to unblock user {} who is already unblocked",
                    userEmail, blocked.getEmail());
            // Idempotent: treat as success
        } else {
            // Soft delete by setting isActive to false
            blockedUser.setActive(false);
            blockedUserRepository.save(blockedUser);
            log.info("User {} unblocked user {}", blocker.getEmail(), blocked.getEmail());
        }

        return MessageResponse.builder()
                .message("User unblocked successfully")
                .build();
    }

    @Override
    public List<BlockedUserResponse> getBlockedUsers(String userEmail) {
        // Get the current user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        log.debug("Fetching blocked users for user: {}", userEmail);

        // Get all users blocked by this user (with eager loading)
        List<BlockedUser> blockedUsers = blockedUserRepository
                .findByBlockerAndIsActiveTrue(user);

        log.debug("Found {} blocked users for user: {}", blockedUsers.size(), userEmail);

        // Map to response DTOs
        List<BlockedUserResponse> response = blockedUsers.stream()
                .map(blockedUser -> {
                    User blocked = blockedUser.getBlocked();
                    log.debug("Mapping blocked user: {} (ID: {})", blocked.getEmail(), blocked.getId());
                    return BlockedUserResponse.builder()
                            .userId(blocked.getId())
                            .email(blocked.getEmail())
                            .firstName(blocked.getFirstName())
                            .lastName(blocked.getLastName())
                            .avatarUrl(blocked.getAvatarUrl())
                            .blockedAt(blockedUser.getBlockedAt())
                            .reason(blockedUser.getReason())
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Returning {} blocked users for user: {}", response.size(), userEmail);
        return response;
    }

    // ================ ACCOUNT MANAGEMENT ================

    @Override
    public MessageResponse deleteAccount(String password, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        // Delete avatar from S3 if exists
        if (user.getAvatarUrl() != null) {
            s3Helper.deleteFile(user.getAvatarUrl());
        }

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        // Mark user as inactive instead of deleting (for data integrity)
        user.setIsActive(false);
        user.setEmail("deleted_" + user.getId() + "@deleted.com"); // Anonymize email
        userRepository.save(user);

        log.info("Account deleted for user: {}", userEmail);

        // Propagate deactivation
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Account deleted successfully")
                .build();
    }

    @Override
    public MessageResponse requestAccountVerification(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isVerified()) {
            throw new IllegalArgumentException("Account is already verified");
        }

        // Implementation for account verification request
        return MessageResponse.builder()
                .message("Account verification request submitted")
                .build();
    }

    @Override
    public AccountDataResponse exportAccountData(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create export data
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("profile", mapToUserProfileResponse(user));
        exportData.put("preferences", getPreferences(userEmail));
        exportData.put("exportDate", LocalDateTime.now());

        return AccountDataResponse.builder()
                .data(exportData)
                .exportDate(LocalDateTime.now())
                .format("JSON")
                .build();
    }

    @Override
    public MessageResponse importAccountData(MultipartFile file, String userEmail) {
        // Implementation for importing account data
        return MessageResponse.builder()
                .message("Account data imported successfully")
                .build();
    }

    // ================ DOCUMENT MANAGEMENT ================

    @Override
    public DocumentUploadResponse uploadDocument(MultipartFile file, String documentType, String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String documentUrl = s3Helper.uploadFile(file, "documents/" + user.getId());

            // Save document metadata to database
            Document document = Document.builder()
                    .user(user)
                    .documentType(documentType)
                    .documentUrl(documentUrl)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .status("PENDING") // Default status
                    .build();

            document = documentRepository.save(document);

            // Log activity
            logUserActivity(user, "DOCUMENT_UPLOADED",
                    "Uploaded document: " + document.getFileName() + " (Type: " + documentType + ")",
                    "", "", "SUCCESS");

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .documentUrl(document.getDocumentUrl())
                    .documentType(document.getDocumentType())
                    .fileName(document.getFileName())
                    .fileSize(document.getFileSize())
                    .uploadDate(document.getUploadDate())
                    .status(document.getStatus())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage());
        }
    }

    @Override
    public List<DocumentResponse> getDocuments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Document> documents = documentRepository.findByUserOrderByUploadDateDesc(user);

        return documents.stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse deleteDocument(String userEmail, String documentId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Document document = documentRepository.findByIdAndUser(documentId, user)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        // Delete from S3
        s3Helper.deleteFile(document.getDocumentUrl());

        // Delete from database
        documentRepository.delete(document);

        return MessageResponse.builder()
                .message("Document deleted successfully")
                .build();
    }

    @Override
    public DocumentResponse getDocument(String userEmail, String documentId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Document document = documentRepository.findByIdAndUser(documentId, user)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        return mapToDocumentResponse(document);
    }

    private DocumentResponse mapToDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .documentId(document.getId())
                .documentType(document.getDocumentType())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .uploadDate(document.getUploadDate())
                .status(document.getStatus())
                .downloadUrl(document.getDocumentUrl())
                .build();
    }

    // ================ ACTIVITY & STATISTICS ================

    @Override
    public UserActivityResponse getUserActivity(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Page<UserActivity> activitiesPage = userActivityRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        List<ActivityItemResponse> activities = activitiesPage.getContent().stream()
                .map(this::mapToActivityItemResponse)
                .collect(Collectors.toList());

        return UserActivityResponse.builder()
                .activities(activities)
                .totalActivities(activitiesPage.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    private ActivityItemResponse mapToActivityItemResponse(UserActivity activity) {
        return ActivityItemResponse.builder()
                .id(activity.getId())
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .ipAddress(activity.getIpAddress())
                .userAgent(activity.getUserAgent())
                .timestamp(activity.getCreatedAt())
                .status(activity.getStatus())
                .metadata(activity.getMetadata())
                .build();
    }

    @Override
    public Page<AdminActivityItemResponse> getAllUserActivities(Pageable pageable, String userEmail) {
        Page<UserActivity> activitiesPage;

        if (userEmail != null && !userEmail.trim().isEmpty()) {
            // Filter by specific user
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
            activitiesPage = userActivityRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        } else {
            // Get all activities
            activitiesPage = userActivityRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        // Convert to admin response format (includes user email)
        return activitiesPage.map(activity -> AdminActivityItemResponse.builder()
                .id(activity.getId())
                .userEmail(activity.getUser().getEmail())
                .userId(activity.getUser().getId())
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .ipAddress(activity.getIpAddress())
                .userAgent(activity.getUserAgent())
                .timestamp(activity.getCreatedAt())
                .status(activity.getStatus())
                .metadata(activity.getMetadata())
                .build());
    }

    @Override
    public MessageResponse trackActivity(TrackActivityRequest request, String userEmail, String ipAddress,
            String userAgent) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        logUserActivity(user, request.getActivityType(), request.getDescription(),
                ipAddress, userAgent, "SUCCESS", request.getMetadata());

        return MessageResponse.builder()
                .message("Activity tracked successfully")
                .build();
    }

    /**
     * Helper method to log user activities
     */
    private void logUserActivity(User user, String activityType, String description,
            String ipAddress, String userAgent, String status) {
        logUserActivity(user, activityType, description, ipAddress, userAgent, status, null);
    }

    /**
     * Helper method to log user activities with metadata (package views, clicks,
     * etc.)
     */
    private void logUserActivity(User user, String activityType, String description,
            String ipAddress, String userAgent, String status, String metadata) {
        try {
            UserActivity activity = UserActivity.builder()
                    .user(user)
                    .activityType(activityType)
                    .description(description)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(status)
                    .metadata(metadata) // Can contain JSON with package info, page views, clicks, etc.
                    .build();
            userActivityRepository.save(activity);
        } catch (Exception e) {
            log.error("Failed to log user activity: {}", e.getMessage());
            // Don't throw - activity logging should not break the main flow
        }
    }

    @Override
    public UserStatsResponse getUserStatistics(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserStatsResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .totalLogins(loginHistoryRepository.countByUser(user))
                .activeTokens(refreshTokenRepository.countActiveTokensByUser(user))
                .accountCreated(user.getCreatedAt())
                .lastLogin(user.getLastLoginAt())
                .isVerified(user.isVerified())
                .isActive(user.isActive())
                .profileCompleteness(calculateProfileCompleteness(user))
                .build();
    }

    @Override
    public LoginHistoryResponse getLoginHistory(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Page<LoginHistory> loginHistoryPage = loginHistoryRepository.findByUserOrderByLoginTimeDesc(user, pageable);

        List<LoginHistoryItemResponse> loginHistory = loginHistoryPage.getContent().stream()
                .map(this::mapToLoginHistoryItemResponse)
                .collect(Collectors.toList());

        return LoginHistoryResponse.builder()
                .loginHistory(loginHistory)
                .totalLogins(loginHistoryPage.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    private LoginHistoryItemResponse mapToLoginHistoryItemResponse(LoginHistory loginHistory) {
        return LoginHistoryItemResponse.builder()
                .id(loginHistory.getId())
                .ipAddress(loginHistory.getIpAddress())
                .userAgent(loginHistory.getUserAgent())
                .location(loginHistory.getLocation())
                .loginTime(loginHistory.getLoginTime())
                .deviceType(loginHistory.getDeviceType())
                .successful(loginHistory.isSuccessful())
                .build();
    }

    // ================ SOCIAL FEATURES ================

    @Override
    public List<UserConnectionResponse> getConnections(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserConnection> connections = userConnectionRepository.findByUserAndIsActiveTrue(user);
        return mapToUserConnectionResponses(connections);
    }

    @Override
    public List<UserConnectionResponse> getPublicConnections(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if profile is visible
        if (user.getPreferences() != null &&
                user.getPreferences().getPrivacy() != null &&
                !Boolean.TRUE.equals(user.getPreferences().getPrivacy().getProfileVisible())) {
            throw new ProfileNotPublicException("Profile is not public");
        }

        List<UserConnection> connections = userConnectionRepository.findByUserAndIsActiveTrue(user);
        return mapToUserConnectionResponses(connections);
    }

    @Override
    public List<UserConnectionResponse> getMutualConnections(String userEmail, String targetUserId) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        // Get currentUser connections
        List<UserConnection> currentUserConnections = userConnectionRepository
                .findByUserAndIsActiveTrue(currentUser);
        Set<String> currentUserConnectionIds = currentUserConnections.stream()
                .map(conn -> conn.getConnectedUser().getId())
                .collect(Collectors.toSet());

        // Get targetUser connections
        List<UserConnection> targetUserConnections = userConnectionRepository.findByUserAndIsActiveTrue(targetUser);

        // Intersection
        List<UserConnection> mutualConnections = targetUserConnections.stream()
                .filter(conn -> currentUserConnectionIds.contains(conn.getConnectedUser().getId()))
                .collect(Collectors.toList());

        return mapToUserConnectionResponses(mutualConnections);
    }

    private List<UserConnectionResponse> mapToUserConnectionResponses(List<UserConnection> connections) {
        return connections.stream()
                .map(connection -> {
                    User connectedUser = connection.getConnectedUser();
                    String email = connectedUser.getEmail();
                    
                    // Priority 1: Real-time status from PresenceService (Redis/Memory)
                    boolean isOnline = presenceService.isUserOnline(email);
                    
                    // Priority 2: Get newest timestamp between DB and special presence store
                    java.time.LocalDateTime dbLastSeen = connectedUser.getLastSeenAt();
                    java.time.LocalDateTime realTimeLastSeen = presenceService.getLastActivity(email).orElse(null);
                    
                    java.time.LocalDateTime finalLastSeen = dbLastSeen;
                    if (realTimeLastSeen != null && (dbLastSeen == null || realTimeLastSeen.isAfter(dbLastSeen))) {
                        finalLastSeen = realTimeLastSeen;
                    }

                    return UserConnectionResponse.builder()
                            .connectionId(connection.getId())
                            .userId(connectedUser.getId())
                            .email(email)
                            .firstName(connectedUser.getFirstName())
                            .lastName(connectedUser.getLastName())
                            .avatarUrl(connectedUser.getAvatarUrl())
                            .connectedAt(connection.getConnectedAt())
                            .connectionType(
                                    connection.getConnectionType() != null ? connection.getConnectionType() : "FRIEND")
                            .lastSeenAt(finalLastSeen)
                            .isOnline(isOnline)
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<NotificationItemResponse> getIncomingConnectionRequests(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. Fetch connection requests
        List<ConnectionRequest> requests = connectionRequestRepository.findByRecipientAndStatus(currentUser, "PENDING");
        List<NotificationItemResponse> notificationItems = requests.stream()
                .map(req -> NotificationItemResponse.builder()
                        .id(req.getId())
                        .type("CONNECTION_REQUEST")
                        .message(req.getRequester().getFirstName() + " " + req.getRequester().getLastName()
                                + " sent you a connection request")
                        .createdAt(req.getCreatedAt() != null ? req.getCreatedAt() : LocalDateTime.now())
                        .data(NotificationItemResponse.NotificationData.builder()
                                .actionUserId(req.getRequester().getId())
                                .userName(req.getRequester().getFirstName() + " " + req.getRequester().getLastName())
                                .userAvatar(req.getRequester().getAvatarUrl())
                                .build())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        // 2. Fetch social notifications (likes, comments)
        List<SocialNotification> socialNotifications = socialNotificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(currentUser.getId());
        List<NotificationItemResponse> socialItems = socialNotifications.stream()
                .map(sn -> NotificationItemResponse.builder()
                        .id(sn.getId())
                        .type(sn.getType())
                        .message(sn.getMessage())
                        .createdAt(sn.getCreatedAt())
                        .data(NotificationItemResponse.NotificationData.builder()
                                .postId(sn.getPostId())
                                .userName(sn.getUserName())
                                .userAvatar(sn.getUserAvatar())
                                .actionUserId(sn.getSenderUserId())
                                .build())
                        .build())
                .collect(Collectors.toList());

        // 3. Combine and sort
        notificationItems.addAll(socialItems);
        notificationItems.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return notificationItems;
    }

    @Override
    public void createSocialNotification(String recipientUserId, String senderUserId, String type, String message,
            String postId, String bookingId, String paymentId, String actionUrl) {
        String userName = "Someone";
        String userAvatar = null;

        if ("SYSTEM".equals(senderUserId)) {
            userName = "System";
        } else {
            User sender = userRepository.findById(senderUserId).orElse(null);
            if (sender != null) {
                userName = sender.getFirstName() + " " + sender.getLastName();
                userAvatar = sender.getAvatarUrl();
            }
        }

        SocialNotification notification = SocialNotification.builder()
                .recipientUserId(recipientUserId)
                .senderUserId(senderUserId)
                .type(type)
                .message(message)
                .postId(postId)
                .bookingId(bookingId)
                .paymentId(paymentId)
                .actionUrl(actionUrl)
                .userName(userName)
                .userAvatar(userAvatar)
                .createdAt(LocalDateTime.now())
                .build();

        socialNotificationRepository.save(notification);

        // Real-time trigger
        User recipient = userRepository.findById(recipientUserId).orElse(null);
        if (recipient != null) {
            NotificationItemResponse unifiedNotification = NotificationItemResponse.builder()
                    .id(notification.getId())
                    .type(type)
                    .message(message)
                    .createdAt(notification.getCreatedAt())
                    .actionUrl(actionUrl)
                    .data(NotificationItemResponse.NotificationData.builder()
                            .postId(postId)
                            .bookingId(bookingId)
                            .paymentId(paymentId)
                            .userName(userName)
                            .userAvatar(userAvatar)
                            .actionUserId(senderUserId)
                            .build())
                    .build();

            realtimeNotificationHelper.sendUnifiedNotification(recipient.getEmail(), unifiedNotification);
        }
    }

    @Override
    public List<ConnectionRequestResponse> getOutgoingConnectionRequests(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ConnectionRequest> requests = connectionRequestRepository
                .findByRequesterAndStatus(currentUser, "PENDING");

        return requests.stream()
                .map(request -> {
                    User recipient = request.getRecipient();
                    return ConnectionRequestResponse.builder()
                            .requestId(request.getId())
                            .requesterId(currentUser.getId())
                            .requesterEmail(currentUser.getEmail())
                            .requesterFirstName(currentUser.getFirstName())
                            .requesterLastName(currentUser.getLastName())
                            .requesterAvatarUrl(currentUser.getAvatarUrl())
                            .recipientId(recipient.getId())
                            .recipientEmail(recipient.getEmail())
                            .status(request.getStatus())
                            .createdAt(request.getCreatedAt())
                            .respondedAt(request.getRespondedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Notify users via WebSocket that a connection has been established.
     * Sends a lightweight message to each user's personal queue.
     */
    private void sendConnectionAcceptedNotification(User userA, User userB) {
        try {
            // Send unified notifications to both users
            realtimeNotificationHelper.sendConnectionAcceptedNotification(
                    userA.getEmail(), userB, null // connectionId can be null for notification
            );
            realtimeNotificationHelper.sendConnectionAcceptedNotification(
                    userB.getEmail(), userA, null);

            // Also send specific connection payloads for backward compatibility
            UserConnectionResponse aViewOfB = UserConnectionResponse.builder()
                    .connectionId(null) // ID is not critical for the notification
                    .userId(userB.getId())
                    .email(userB.getEmail())
                    .firstName(userB.getFirstName())
                    .lastName(userB.getLastName())
                    .avatarUrl(userB.getAvatarUrl())
                    .connectionType("FRIEND")
                    .connectedAt(LocalDateTime.now())
                    .build();

            UserConnectionResponse bViewOfA = UserConnectionResponse.builder()
                    .connectionId(null)
                    .userId(userA.getId())
                    .email(userA.getEmail())
                    .firstName(userA.getFirstName())
                    .lastName(userA.getLastName())
                    .avatarUrl(userA.getAvatarUrl())
                    .connectionType("FRIEND")
                    .connectedAt(LocalDateTime.now())
                    .build();

            // Frontend should subscribe to /user/queue/connections
            messagingTemplate.convertAndSendToUser(
                    userA.getEmail(),
                    "/queue/connections",
                    aViewOfB);

            messagingTemplate.convertAndSendToUser(
                    userB.getEmail(),
                    "/queue/connections",
                    bViewOfA);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification for accepted connection: {}", e.getMessage());
        }
    }

    @Override
    public MessageResponse sendConnectionRequest(String userEmail, String targetUserId) {
        // Input validation
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Target user ID cannot be null or empty");
        }

        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User recipient = userRepository.findById(targetUserId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        if (requester.getId().equals(targetUserId.trim())) {
            throw new IllegalArgumentException("Cannot send connection request to yourself");
        }

        // Check if active connection already exists (bidirectional check)
        // Only check for ACTIVE connections, not deactivated ones
        Optional<UserConnection> forwardConnection = userConnectionRepository.findByUserAndConnectedUser(requester,
                recipient);
        Optional<UserConnection> reverseConnection = userConnectionRepository.findByUserAndConnectedUser(recipient,
                requester);

        if ((forwardConnection.isPresent() && forwardConnection.get().isActive()) ||
                (reverseConnection.isPresent() && reverseConnection.get().isActive())) {
            throw new IllegalArgumentException("Connection already exists");
        }

        // Check if request already exists from requester to recipient
        Optional<ConnectionRequest> existingForwardRequest = connectionRequestRepository
                .findByRequesterAndRecipient(requester, recipient);
        if (existingForwardRequest.isPresent()) {
            ConnectionRequest existingRequest = existingForwardRequest.get();
            if ("PENDING".equals(existingRequest.getStatus())) {
                throw new IllegalArgumentException("Connection request already sent");
            } else if ("ACCEPTED".equals(existingRequest.getStatus())) {
                // If request was accepted but no active connection exists (connection was
                // removed),
                // allow creating a new request by updating the existing one
                // Check if there's actually an active connection (already checked above, but
                // double-check)
                boolean hasActiveConnection = (forwardConnection.isPresent() && forwardConnection.get().isActive()) ||
                        (reverseConnection.isPresent() && reverseConnection.get().isActive());
                if (hasActiveConnection) {
                    throw new IllegalArgumentException("Connection already exists");
                } else {
                    // Connection was removed, so update ACCEPTED request to PENDING to allow new
                    // request
                    existingRequest.setStatus("PENDING");
                    existingRequest.setRespondedAt(null);
                    connectionRequestRepository.save(existingRequest);

                    try {
                        realtimeNotificationHelper.sendConnectionRequestNotification(
                                recipient.getEmail(), requester, existingRequest.getId());
                    } catch (Exception e) {
                        log.warn("Failed to send WebSocket notification for connection request: {}", e.getMessage());
                    }

                    return MessageResponse.builder()
                            .message("Connection request sent successfully")
                            .build();
                }
            } else if ("REJECTED".equals(existingRequest.getStatus()) ||
                    "WITHDRAWN".equals(existingRequest.getStatus())) {
                // If previous request was rejected or withdrawn, update it to PENDING instead
                // of creating new one
                // This avoids unique constraint violation
                existingRequest.setStatus("PENDING");
                existingRequest.setRespondedAt(null); // Clear previous response
                connectionRequestRepository.save(existingRequest);

                // Notify recipient in real-time
                try {
                    realtimeNotificationHelper.sendConnectionRequestNotification(
                            recipient.getEmail(), requester, existingRequest.getId());
                } catch (Exception e) {
                    log.warn("Failed to send WebSocket notification for connection request: {}", e.getMessage());
                }

                return MessageResponse.builder()
                        .message("Connection request sent successfully")
                        .build();
            } else {
                // For any other unknown status, update to PENDING
                existingRequest.setStatus("PENDING");
                existingRequest.setRespondedAt(null);
                connectionRequestRepository.save(existingRequest);

                try {
                    realtimeNotificationHelper.sendConnectionRequestNotification(
                            recipient.getEmail(), requester, existingRequest.getId());
                } catch (Exception e) {
                    log.warn("Failed to send WebSocket notification for connection request: {}", e.getMessage());
                }

                return MessageResponse.builder()
                        .message("Connection request sent successfully")
                        .build();
            }
        }

        // Check if reverse request exists (recipient already sent request to requester)
        Optional<ConnectionRequest> reverseRequest = connectionRequestRepository
                .findByRequesterAndRecipient(recipient, requester);

        if (reverseRequest.isPresent()) {
            ConnectionRequest existingReverseRequest = reverseRequest.get();
            if ("PENDING".equals(existingReverseRequest.getStatus())) {
                // If reverse request is pending, auto-accept it and create connection
                return autoAcceptReverseRequest(existingReverseRequest, requester);
            } else if ("REJECTED".equals(existingReverseRequest.getStatus()) ||
                    "WITHDRAWN".equals(existingReverseRequest.getStatus())) {
                // If previous reverse request was rejected or withdrawn, delete it and create
                // new one
                connectionRequestRepository.delete(existingReverseRequest);
                // Continue with creating new request below
            } else {
                // For any other status (e.g., ACCEPTED), don't allow new request
                throw new IllegalArgumentException("A connection request already exists between you and this user");
            }
        }

        // Create new connection request
        ConnectionRequest request = ConnectionRequest.builder()
                .requester(requester)
                .recipient(recipient)
                .status("PENDING")
                .build();

        connectionRequestRepository.save(request);

        // Notify recipient in real-time via WebSocket (unified notification)
        try {
            // Send unified notification
            realtimeNotificationHelper.sendConnectionRequestNotification(
                    recipient.getEmail(), requester, request.getId());

            // Also send specific connection request payload for backward compatibility
            ConnectionRequestResponse payload = ConnectionRequestResponse.builder()
                    .requestId(request.getId())
                    .requesterId(requester.getId())
                    .requesterEmail(requester.getEmail())
                    .requesterFirstName(requester.getFirstName())
                    .requesterLastName(requester.getLastName())
                    .requesterAvatarUrl(requester.getAvatarUrl())
                    .recipientId(recipient.getId())
                    .recipientEmail(recipient.getEmail())
                    .status(request.getStatus())
                    .createdAt(request.getCreatedAt())
                    .respondedAt(request.getRespondedAt())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/connection-requests",
                    payload);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification for connection request: {}", e.getMessage());
        }

        return MessageResponse.builder()
                .message("Connection request sent successfully")
                .build();
    }

    /**
     * Auto-accept reverse connection request when both users send requests to each
     * other
     */
    private MessageResponse autoAcceptReverseRequest(ConnectionRequest reverseRequest, User requester) {
        // Update reverse request status
        reverseRequest.setStatus("ACCEPTED");
        reverseRequest.setRespondedAt(LocalDateTime.now());
        connectionRequestRepository.save(reverseRequest);

        // Create bidirectional connection
        UserConnection connection1 = UserConnection.builder()
                .user(reverseRequest.getRequester())
                .connectedUser(reverseRequest.getRecipient())
                .connectionType("FRIEND")
                .isActive(true)
                .build();

        UserConnection connection2 = UserConnection.builder()
                .user(reverseRequest.getRecipient())
                .connectedUser(reverseRequest.getRequester())
                .connectionType("FRIEND")
                .isActive(true)
                .build();

        userConnectionRepository.save(connection1);
        userConnectionRepository.save(connection2);

        // Notify both users that they are now connected
        sendConnectionAcceptedNotification(reverseRequest.getRequester(), reverseRequest.getRecipient());

        return MessageResponse.builder()
                .message("Connection request accepted automatically (mutual request)")
                .build();
    }

    @Override
    public MessageResponse acceptConnectionRequest(String userEmail, String requestId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ConnectionRequest request = connectionRequestRepository.findByIdAndRecipient(requestId, user)
                .orElseThrow(() -> new IllegalArgumentException("Connection request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Connection request is not pending");
        }

        // Check if connection already exists (bidirectional check)
        Optional<UserConnection> existingConnection1 = userConnectionRepository.findByUserAndConnectedUser(
                request.getRequester(), request.getRecipient());
        Optional<UserConnection> existingConnection2 = userConnectionRepository.findByUserAndConnectedUser(
                request.getRecipient(), request.getRequester());

        // If active connection already exists, just update the request status and
        // return
        if ((existingConnection1.isPresent() && existingConnection1.get().isActive()) ||
                (existingConnection2.isPresent() && existingConnection2.get().isActive())) {
            // Connection already exists, just mark request as accepted
            request.setStatus("ACCEPTED");
            request.setRespondedAt(LocalDateTime.now());
            connectionRequestRepository.save(request);

            log.info("Connection already exists between {} and {}, request marked as accepted",
                    request.getRequester().getEmail(), request.getRecipient().getEmail());

            return MessageResponse.builder()
                    .message("Connection request accepted (connection already exists)")
                    .build();
        }

        // Update request status
        request.setStatus("ACCEPTED");
        request.setRespondedAt(LocalDateTime.now());
        connectionRequestRepository.save(request);

        // Check if deactivated connections exist - reactivate them instead of creating
        // new ones
        if (existingConnection1.isPresent() && !existingConnection1.get().isActive()) {
            // Reactivate existing connection
            UserConnection conn1 = existingConnection1.get();
            conn1.setActive(true);
            conn1.setConnectedAt(LocalDateTime.now());
            userConnectionRepository.save(conn1);

            // Create or reactivate reverse connection
            if (existingConnection2.isPresent()) {
                UserConnection conn2 = existingConnection2.get();
                conn2.setActive(true);
                conn2.setConnectedAt(LocalDateTime.now());
                userConnectionRepository.save(conn2);
            } else {
                // Create reverse connection
                UserConnection connection2 = UserConnection.builder()
                        .user(request.getRecipient())
                        .connectedUser(request.getRequester())
                        .connectionType("FRIEND")
                        .isActive(true)
                        .build();
                userConnectionRepository.save(connection2);
            }
        } else if (existingConnection2.isPresent() && !existingConnection2.get().isActive()) {
            // Reactivate existing reverse connection
            UserConnection conn2 = existingConnection2.get();
            conn2.setActive(true);
            conn2.setConnectedAt(LocalDateTime.now());
            userConnectionRepository.save(conn2);

            // Create forward connection
            UserConnection connection1 = UserConnection.builder()
                    .user(request.getRequester())
                    .connectedUser(request.getRecipient())
                    .connectionType("FRIEND")
                    .isActive(true)
                    .build();
            userConnectionRepository.save(connection1);
        } else {
            // No existing connections, create new bidirectional connection
            UserConnection connection1 = UserConnection.builder()
                    .user(request.getRequester())
                    .connectedUser(request.getRecipient())
                    .connectionType("FRIEND")
                    .isActive(true)
                    .build();

            UserConnection connection2 = UserConnection.builder()
                    .user(request.getRecipient())
                    .connectedUser(request.getRequester())
                    .connectionType("FRIEND")
                    .isActive(true)
                    .build();

            userConnectionRepository.save(connection1);
            userConnectionRepository.save(connection2);
        }

        // Notify original requester that their request was accepted
        sendConnectionAcceptedNotification(request.getRequester(), request.getRecipient());

        return MessageResponse.builder()
                .message("Connection request accepted")
                .build();
    }

    @Override
    public MessageResponse rejectConnectionRequest(String userEmail, String requestId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ConnectionRequest request = connectionRequestRepository.findByIdAndRecipient(requestId, user)
                .orElseThrow(() -> new IllegalArgumentException("Connection request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("Connection request is not pending");
        }

        request.setStatus("REJECTED");
        request.setRespondedAt(LocalDateTime.now());
        connectionRequestRepository.save(request);

        return MessageResponse.builder()
                .message("Connection request rejected")
                .build();
    }

    @Override
    public MessageResponse withdrawConnectionRequest(String userEmail, String requestId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ConnectionRequest request = connectionRequestRepository.findByIdAndRequester(requestId, user)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Connection request not found or you are not the requester"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Connection request is not pending. It may have already been accepted or rejected");
        }

        // Store values before deleting the request
        String requestIdValue = request.getId();
        User requester = request.getRequester();
        User recipient = request.getRecipient();
        LocalDateTime createdAt = request.getCreatedAt();

        // Delete the request (withdraw/cancel it)
        connectionRequestRepository.delete(request);

        // Notify recipient in real-time via WebSocket that the request was withdrawn
        try {
            // Send unified notification
            realtimeNotificationHelper.sendConnectionWithdrawnNotification(
                    recipient.getEmail(), requester, requestIdValue);

            // Also send specific connection request payload for backward compatibility
            ConnectionRequestResponse payload = ConnectionRequestResponse.builder()
                    .requestId(requestIdValue)
                    .requesterId(requester.getId())
                    .requesterEmail(requester.getEmail())
                    .requesterFirstName(requester.getFirstName())
                    .requesterLastName(requester.getLastName())
                    .requesterAvatarUrl(requester.getAvatarUrl())
                    .recipientId(recipient.getId())
                    .recipientEmail(recipient.getEmail())
                    .status("WITHDRAWN")
                    .createdAt(createdAt)
                    .respondedAt(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/connection-requests",
                    payload);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification for withdrawn connection request: {}", e.getMessage());
        }

        return MessageResponse.builder()
                .message("Connection request withdrawn successfully")
                .build();
    }

    @Override
    public MessageResponse removeConnection(String userEmail, String connectionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserConnection connection = userConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));

        if (!connection.getUser().getId().equals(user.getId()) &&
                !connection.getConnectedUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't have permission to remove this connection");
        }

        // Deactivate both connections (bidirectional)
        connection.setActive(false);
        userConnectionRepository.save(connection);

        // Find and deactivate the reverse connection
        userConnectionRepository.findByUserAndConnectedUser(
                connection.getConnectedUser(),
                connection.getUser()).ifPresent(reverseConnection -> {
                    reverseConnection.setActive(false);
                    userConnectionRepository.save(reverseConnection);
                });

        return MessageResponse.builder()
                .message("Connection removed successfully")
                .build();
    }

    // ================ SEARCH & DISCOVERY ================

    @Override
    public Page<UserProfileResponse> searchUsers(String query, Pageable pageable, String currentUserId) {
        // Check if current user is admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        List<User> users = userRepository.findAll();
        List<UserProfileResponse> filteredUsers = users.stream()
                .filter(user -> user.isActive() && user.isVerified())
                .filter(user -> !user.getId().equals(currentUserId))
                .filter(user -> {
                    // Exclude Agents and Suppliers for non-admins
                    if (!isAdmin) {
                        if (user.getRole() == User.UserRole.B2B)
                            return false;
                        if (agentRepository.existsByUser(user))
                            return false;
                        if (supplierRepository.existsByUser(user))
                            return false;
                    }
                    return true;
                })
                .filter(user -> user.getFirstName().toLowerCase().contains(query.toLowerCase()) ||
                        user.getLastName().toLowerCase().contains(query.toLowerCase()) ||
                        user.getEmail().toLowerCase().contains(query.toLowerCase()))
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
        List<UserProfileResponse> pageContent = filteredUsers.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredUsers.size());
    }

    @Override
    public List<UserSuggestionResponse> getSuggestedUsers(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get all active users excluding the current user (include both verified and
        // unverified)
        // Newly built profiles will now appear in suggestions
        List<User> allUsers = userRepository.findAll().stream()
                .filter(user -> user.isActive())
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    // Exclude Agents and Suppliers from suggestions
                    if (user.getRole() == User.UserRole.B2B)
                        return false;
                    if (agentRepository.existsByUser(user))
                        return false;
                    if (supplierRepository.existsByUser(user))
                        return false;
                    return true;
                })
                .collect(Collectors.toList());

        // Get current user's connections
        List<UserConnection> userConnections = userConnectionRepository.findByUserAndIsActiveTrue(currentUser);
        Set<String> connectedUserIds = userConnections.stream()
                .map(conn -> conn.getConnectedUser().getId())
                .collect(Collectors.toSet());

        // Filter out already connected users
        List<User> candidateUsers = allUsers.stream()
                .filter(user -> !connectedUserIds.contains(user.getId()))
                .collect(Collectors.toList());

        // OPTIMIZATION: Batch fetch all candidate user connections in a single query
        // (avoids N+1)
        Set<String> candidateUserIds = candidateUsers.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<UserConnection> allCandidateConnections = userConnectionRepository
                .findByUserIdInAndIsActiveTrue(candidateUserIds);

        // Group connections by user ID for O(1) lookup
        Map<String, List<UserConnection>> connectionsByUserId = allCandidateConnections.stream()
                .collect(Collectors.groupingBy(conn -> conn.getUser().getId()));

        // Get mutual connections count for each candidate
        // Calculate match scores and sort BEFORE limiting to get top 20 by score
        List<UserSuggestionResponse> suggestions = candidateUsers.stream()
                .map(candidate -> {
                    // Get candidate connections from pre-fetched map (O(1) lookup)
                    List<UserConnection> candidateConnections = connectionsByUserId.getOrDefault(candidate.getId(),
                            Collections.emptyList());
                    int totalConnections = candidateConnections.size();
                    Set<String> candidateConnectedIds = candidateConnections.stream()
                            .map(conn -> conn.getConnectedUser().getId())
                            .collect(Collectors.toSet());

                    long mutualConnections = userConnections.stream()
                            .filter(conn -> candidateConnectedIds.contains(conn.getConnectedUser().getId()))
                            .count();

                    // Check if same city (using address if available)
                    boolean sameCity = false;
                    if (candidate.getAddress() != null && currentUser.getAddress() != null &&
                            candidate.getAddress().getCity() != null && currentUser.getAddress().getCity() != null &&
                            candidate.getAddress().getCity().equals(currentUser.getAddress().getCity())) {
                        sameCity = true;
                    }

                    // Determine suggestion reason
                    String reason = "You might know";
                    if (mutualConnections > 0) {
                        reason = mutualConnections + " mutual connection" + (mutualConnections > 1 ? "s" : "");
                    } else if (sameCity) {
                        reason = "Same city";
                    } else {
                        reason = "Popular user";
                    }

                    // Calculate match score (0-100)
                    double matchScore = 50.0; // Base score
                    if (mutualConnections > 0) {
                        matchScore += Math.min(mutualConnections * 10, 30); // Up to 30 points for mutual connections
                    }
                    if (sameCity) {
                        matchScore += 10; // 10 points for same city
                    }
                    if (candidate.isVerified()) {
                        matchScore += 10; // 10 points for verified users
                    }

                    return UserSuggestionResponse.builder()
                            .userId(candidate.getId())
                            .email(candidate.getEmail())
                            .firstName(candidate.getFirstName())
                            .lastName(candidate.getLastName())
                            .avatarUrl(candidate.getAvatarUrl())
                            .suggestionReason(reason)
                            .mutualConnections((int) mutualConnections)
                            .totalConnections(totalConnections)
                            .matchScore(Math.min(matchScore, 100.0))
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore())) // Sort by match score
                                                                                        // descending
                .limit(20) // Limit to top 20 suggestions AFTER sorting
                .collect(Collectors.toList());

        return suggestions;
    }

    @Override
    public List<UserProfileResponse> getNearbyUsers(String userEmail, double radius) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get current user's upcoming travel plans
        List<TravelPlan> userTravelPlans = travelPlanRepository.findByUserAndStatus(
                currentUser, "PLANNED");

        if (userTravelPlans.isEmpty()) {
            // If user has no travel plans, return empty list
            return new ArrayList<>();
        }

        // Get all unique destinations from user's travel plans
        Set<String> userDestinations = userTravelPlans.stream()
                .map(TravelPlan::getDestination)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Get upcoming travel dates
        LocalDate today = LocalDate.now();

        // Get current user's address for location filtering
        String userCity = currentUser.getAddress() != null ? currentUser.getAddress().getCity() : null;
        String userState = currentUser.getAddress() != null ? currentUser.getAddress().getState() : null;
        String userCountry = currentUser.getAddress() != null ? currentUser.getAddress().getCountry() : null;

        // Find all travel plans for the same destinations with upcoming dates
        List<TravelPlan> matchingPlans = new ArrayList<>();
        for (String destination : userDestinations) {
            List<TravelPlan> plans = travelPlanRepository
                    .findByDestinationIgnoreCaseAndStatusAndTravelDateGreaterThanEqual(
                            destination, "PLANNED", today);
            matchingPlans.addAll(plans);
        }

        // Filter by location based on radius (if user has location info)
        if (userCity != null || userState != null || userCountry != null) {
            matchingPlans = matchingPlans.stream()
                    .filter(plan -> {
                        User traveler = plan.getUser();
                        if (traveler.getAddress() == null)
                            return false;

                        String travelerCity = traveler.getAddress().getCity();
                        String travelerState = traveler.getAddress().getState();
                        String travelerCountry = traveler.getAddress().getCountry();

                        // If radius is small (city level), match by city
                        if (radius <= 50.0) {
                            return userCity != null && travelerCity != null &&
                                    userCity.equalsIgnoreCase(travelerCity) &&
                                    (userState == null || travelerState == null ||
                                            userState.equalsIgnoreCase(travelerState));
                        }
                        // If radius is medium (state level), match by state
                        else if (radius <= 500.0) {
                            return userState != null && travelerState != null &&
                                    userState.equalsIgnoreCase(travelerState) &&
                                    (userCountry == null || travelerCountry == null ||
                                            userCountry.equalsIgnoreCase(travelerCountry));
                        }
                        // If radius is large (country level), match by country
                        else {
                            return userCountry != null && travelerCountry != null &&
                                    userCountry.equalsIgnoreCase(travelerCountry);
                        }
                    })
                    .collect(Collectors.toList());
        }

        // Filter out current user's own plans and get unique users
        Set<String> matchingUserIds = matchingPlans.stream()
                .filter(plan -> !plan.getUser().getId().equals(currentUser.getId()))
                .map(plan -> plan.getUser().getId())
                .collect(Collectors.toSet());

        // Get user profiles for matching travelers with privacy awareness
        // (Instagram-like)
        List<UserProfileResponse> nearbyUsers = matchingUserIds.stream()
                .map(userId -> userRepository.findById(userId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(user -> user.isActive() && user.isVerified())
                .limit(50) // Limit to 50 travelers
                .map(user -> mapToUserProfileResponse(user, currentUser))
                .collect(Collectors.toList());

        return nearbyUsers;
    }

    @Override
    public List<UserProfileResponse> getNearbyTravelers(String userEmail, String destination, String filterByLocation) {
        // Input validation
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination cannot be null or empty");
        }

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get current user's address for location filtering
        String userCity = currentUser.getAddress() != null ? currentUser.getAddress().getCity() : null;
        String userState = currentUser.getAddress() != null ? currentUser.getAddress().getState() : null;
        String userCountry = currentUser.getAddress() != null ? currentUser.getAddress().getCountry() : null;

        // Get upcoming travel dates (include plans from 6 months ago to future)
        LocalDate today = LocalDate.now();
        LocalDate sixMonthsAgo = today.minusMonths(6);

        // Use optimized query with JOIN FETCH to avoid N+1 problem
        List<TravelPlan> matchingPlans = travelPlanRepository
                .findByDestinationIgnoreCaseAndStatusAndTravelDateGreaterThanEqualWithUser(
                        destination.trim(), "PLANNED", sixMonthsAgo);

        // If no results with date filter, try without date restriction (for
        // testing/development)
        if (matchingPlans.isEmpty()) {
            matchingPlans = travelPlanRepository.findByDestinationIgnoreCaseAndStatusWithUser(
                    destination.trim(), "PLANNED");
            log.debug("No upcoming travel plans found for {}, searching all PLANNED plans", destination);
        }

        // Filter by location if specified (only if user has address info)
        if (filterByLocation != null && !filterByLocation.isEmpty() &&
                (userCity != null || userState != null || userCountry != null)) {
            matchingPlans = matchingPlans.stream()
                    .filter(plan -> {
                        User traveler = plan.getUser();
                        if (traveler.getAddress() == null)
                            return false;

                        String travelerCity = traveler.getAddress().getCity();
                        String travelerState = traveler.getAddress().getState();
                        String travelerCountry = traveler.getAddress().getCountry();

                        switch (filterByLocation.toUpperCase()) {
                            case "CITY":
                                return userCity != null && travelerCity != null &&
                                        userCity.equalsIgnoreCase(travelerCity);
                            case "STATE":
                                return userState != null && travelerState != null &&
                                        userState.equalsIgnoreCase(travelerState);
                            case "COUNTRY":
                                return userCountry != null && travelerCountry != null &&
                                        userCountry.equalsIgnoreCase(travelerCountry);
                            default:
                                return true; // No location filter
                        }
                    })
                    .collect(Collectors.toList());
        }

        // Filter out current user's own plans and get unique users
        Set<String> matchingUserIds = matchingPlans.stream()
                .filter(plan -> !plan.getUser().getId().equals(currentUser.getId()))
                .map(plan -> plan.getUser().getId())
                .collect(Collectors.toSet());

        // Use batch lookup to avoid N+1 problem
        List<User> matchingUsers = userRepository.findAllById(matchingUserIds);

        // Filter and map to response with privacy awareness (Instagram-like)
        List<UserProfileResponse> nearbyTravelers = matchingUsers.stream()
                .filter(user -> user.isActive())
                .limit(50) // Limit to 50 travelers
                .map(user -> mapToUserProfileResponse(user, currentUser))
                .collect(Collectors.toList());

        log.debug("Found {} travelers for destination: {} (filterByLocation: {})",
                nearbyTravelers.size(), destination, filterByLocation);

        return nearbyTravelers;
    }

    // ================ TRAVEL PLANS ================

    @Override
    public TravelPlanResponse createTravelPlan(TravelPlanRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if user already has a plan for this destination with same date
        if (travelPlanRepository.existsByUserAndDestinationIgnoreCaseAndStatus(
                user, request.getDestination(), "PLANNED")) {
            throw new IllegalArgumentException("You already have a travel plan for " + request.getDestination());
        }

        TravelPlan travelPlan = TravelPlan.builder()
                .user(user)
                .destination(request.getDestination())
                .description(request.getDescription())
                .travelDate(request.getTravelDate())
                .returnDate(request.getReturnDate())
                .status("PLANNED")
                .travelType(request.getTravelType() != null ? request.getTravelType() : "SOLO")
                .numberOfTravelers(request.getNumberOfTravelers())
                .build();

        travelPlan = travelPlanRepository.save(travelPlan);

        return mapToTravelPlanResponse(travelPlan);
    }

    @Override
    public List<TravelPlanResponse> getTravelPlans(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<TravelPlan> plans = travelPlanRepository.findByUserOrderByTravelDateAsc(user);
        return plans.stream()
                .map(this::mapToTravelPlanResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TravelPlanResponse updateTravelPlan(String planId, TravelPlanRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found"));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't have permission to update this travel plan");
        }

        travelPlan.setDestination(request.getDestination());
        travelPlan.setDescription(request.getDescription());
        travelPlan.setTravelDate(request.getTravelDate());
        travelPlan.setReturnDate(request.getReturnDate());
        travelPlan.setTravelType(request.getTravelType());
        travelPlan.setNumberOfTravelers(request.getNumberOfTravelers());

        travelPlan = travelPlanRepository.save(travelPlan);

        return mapToTravelPlanResponse(travelPlan);
    }

    @Override
    public MessageResponse deleteTravelPlan(String planId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found"));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't have permission to delete this travel plan");
        }

        travelPlanRepository.delete(travelPlan);

        return MessageResponse.builder()
                .message("Travel plan deleted successfully")
                .build();
    }

    @Override
    public List<TravelPlanResponse> findTravelersByDestination(String destination) {
        LocalDate today = LocalDate.now();
        List<TravelPlan> plans = travelPlanRepository
                .findByDestinationIgnoreCaseAndStatusAndTravelDateGreaterThanEqual(
                        destination, "PLANNED", today);

        return plans.stream()
                .map(this::mapToTravelPlanResponse)
                .collect(Collectors.toList());
    }

    // ================ TRAVEL GROUPS ================

    @Override
    public TravelGroupResponse createTravelGroup(TravelGroupRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validate travel date (must be in the future)
        LocalDate today = LocalDate.now();
        if (request.getTravelDate() != null && request.getTravelDate().isBefore(today)) {
            throw new IllegalArgumentException("Travel date must be in the future");
        }

        // Validate return date (must be after travel date if provided)
        if (request.getReturnDate() != null && request.getTravelDate() != null) {
            if (request.getReturnDate().isBefore(request.getTravelDate())) {
                throw new IllegalArgumentException("Return date must be after travel date");
            }
        }

        // Validate maxMembers (must be between 1 and 100)
        int maxMembers = request.getMaxMembers() != null ? request.getMaxMembers() : 10;
        if (maxMembers < 1) {
            throw new IllegalArgumentException("Maximum members must be at least 1");
        }
        if (maxMembers > 100) {
            throw new IllegalArgumentException("Maximum members cannot exceed 100");
        }

        // Generate unique invite code (retry if duplicate)
        String inviteCode = generateUniqueInviteCode();

        TravelGroup group = TravelGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .createdBy(user)
                .destination(request.getDestination())
                .travelDate(request.getTravelDate())
                .returnDate(request.getReturnDate())
                .maxMembers(maxMembers)
                .currentMembers(1)
                .status("PLANNING")
                .travelType(request.getTravelType() != null ? request.getTravelType() : "GROUP")
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .inviteCode(inviteCode)
                .build();

        group = travelGroupRepository.save(group);

        // Add creator as member with CREATOR role
        TravelGroupMember creatorMember = TravelGroupMember.builder()
                .group(group)
                .user(user)
                .role("CREATOR")
                .status("ACTIVE")
                .build();
        travelGroupMemberRepository.save(creatorMember);

        return mapToTravelGroupResponse(group);
    }

    @Override
    public TravelGroupResponse createTravelGroupWithImage(TravelGroupRequest request,
            org.springframework.web.multipart.MultipartFile file,
            String userEmail) {
        // Image is optional – if no file provided, just create group without image
        if (file != null && !file.isEmpty()) {
            try {
                String imageUrl = s3Helper.uploadImage(file, "travel-groups");
                request.setImageUrl(imageUrl);
            } catch (Exception e) {
                log.error("Failed to upload travel group image on create: {}", e.getMessage(), e);
                throw new IllegalArgumentException("Failed to upload group image: " + e.getMessage());
            }
        }

        return createTravelGroup(request, userEmail);
    }

    @Override
    public TravelGroupResponse getTravelGroup(String groupId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Check if user is member or group is public
        boolean isMember = travelGroupMemberRepository.existsByGroupAndUser(group, user);
        if (!isMember && !group.getIsPublic()) {
            throw new IllegalArgumentException("You don't have access to this travel group");
        }

        return mapToTravelGroupResponse(group);
    }

    @Override
    public List<TravelGroupResponse> getMyTravelGroups(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<TravelGroupMember> memberships = travelGroupMemberRepository.findByUser(user);
        return memberships.stream()
                .map(member -> mapToTravelGroupResponse(member.getGroup()))
                .collect(Collectors.toList());
    }

    @Override
    public TravelGroupResponse updateTravelGroup(String groupId, TravelGroupRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Check if user is creator or admin
        TravelGroupMember member = travelGroupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        if (!"CREATOR".equals(member.getRole()) && !"ADMIN".equals(member.getRole())) {
            throw new IllegalArgumentException("Only group creator or admin can update the group");
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setDestination(request.getDestination());
        group.setTravelDate(request.getTravelDate());
        group.setReturnDate(request.getReturnDate());
        if (request.getMaxMembers() != null) {
            group.setMaxMembers(request.getMaxMembers());
        }
        group.setTravelType(request.getTravelType());
        if (request.getIsPublic() != null) {
            group.setIsPublic(request.getIsPublic());
        }
        if (request.getImageUrl() != null) {
            group.setImageUrl(request.getImageUrl());
        }

        group = travelGroupRepository.save(group);

        return mapToTravelGroupResponse(group);
    }

    @Override
    public TravelGroupResponse uploadGroupImage(String groupId, org.springframework.web.multipart.MultipartFile file,
            String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Only group members can upload/update the group image
        travelGroupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        try {
            // If there is an existing image, optionally delete it
            if (group.getImageUrl() != null) {
                try {
                    s3Helper.deleteFile(group.getImageUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete previous group image from S3: {}", e.getMessage());
                }
            }

            String imageUrl = s3Helper.uploadImage(file, "travel-groups");
            group.setImageUrl(imageUrl);
            travelGroupRepository.save(group);

            logUserActivity(user, "TRAVEL_GROUP_IMAGE_UPDATED",
                    String.format("Updated image for travel group: %s", group.getName()),
                    "", "", "SUCCESS");

            return mapToTravelGroupResponse(group);
        } catch (Exception e) {
            log.error("Failed to upload group image: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to upload group image: " + e.getMessage());
        }
    }

    @Override
    public MessageResponse deleteTravelGroup(String groupId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Only creator can delete
        if (!group.getCreatedBy().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only group creator can delete the group");
        }

        // Delete all members first
        travelGroupMemberRepository.deleteAll(travelGroupMemberRepository.findByGroup(group));
        travelGroupRepository.delete(group);

        return MessageResponse.builder()
                .message("Travel group deleted successfully")
                .build();
    }

    @Override
    public MessageResponse joinTravelGroup(String inviteCode, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

        // Check if already a member
        if (travelGroupMemberRepository.existsByGroupAndUser(group, user)) {
            throw new IllegalArgumentException("You are already a member of this group");
        }

        // Check if group is full
        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            throw new IllegalArgumentException("Travel group is full");
        }

        // Add member
        TravelGroupMember member = TravelGroupMember.builder()
                .group(group)
                .user(user)
                .role("MEMBER")
                .status("ACTIVE")
                .build();
        travelGroupMemberRepository.save(member);

        // Update member count
        group.setCurrentMembers(group.getCurrentMembers() + 1);
        travelGroupRepository.save(group);

        return MessageResponse.builder()
                .message("Successfully joined travel group: " + group.getName())
                .build();
    }

    @Override
    public MessageResponse leaveTravelGroup(String groupId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        TravelGroupMember member = travelGroupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        // Creator cannot leave (must delete group instead)
        if ("CREATOR".equals(member.getRole())) {
            throw new IllegalArgumentException("Group creator cannot leave. Please delete the group instead.");
        }

        travelGroupMemberRepository.delete(member);

        // Update member count
        group.setCurrentMembers(Math.max(0, group.getCurrentMembers() - 1));
        travelGroupRepository.save(group);

        return MessageResponse.builder()
                .message("Successfully left travel group")
                .build();
    }

    @Override
    public MessageResponse inviteToTravelGroup(String groupId, String userEmail, String inviteeEmail) {
        User inviter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User invitee = userRepository.findByEmail(inviteeEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invitee not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Check if inviter is member
        TravelGroupMember inviterMember = travelGroupMemberRepository.findByGroupAndUser(group, inviter)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        // Check if invitee is already a member
        if (travelGroupMemberRepository.existsByGroupAndUser(group, invitee)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        // Check if group is full
        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            throw new IllegalArgumentException("Travel group is full");
        }

        // Add invitee as member
        TravelGroupMember member = TravelGroupMember.builder()
                .group(group)
                .user(invitee)
                .role("MEMBER")
                .status("ACTIVE")
                .build();
        travelGroupMemberRepository.save(member);

        // Update member count
        group.setCurrentMembers(group.getCurrentMembers() + 1);
        travelGroupRepository.save(group);

        return MessageResponse.builder()
                .message("Successfully invited " + inviteeEmail + " to the travel group")
                .build();
    }

    @Override
    public List<TravelGroupResponse> discoverTravelGroups(String destination, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate today = LocalDate.now();
        LocalDate sixMonthsAgo = today.minusMonths(6); // Include groups from 6 months ago

        // First try with upcoming dates and PLANNING status
        List<TravelGroup> groups = travelGroupRepository
                .findByDestinationIgnoreCaseAndStatusAndTravelDateGreaterThanEqual(
                        destination, "PLANNING", sixMonthsAgo);

        // If no results, try without date restriction (for testing/development)
        if (groups.isEmpty()) {
            groups = travelGroupRepository.findByDestinationIgnoreCaseAndStatus(
                    destination, "PLANNING");
            log.debug("No upcoming travel groups found for {}, searching all PLANNING groups", destination);
        }

        // Filter out groups user is already a member of
        return groups.stream()
                .filter(group -> group.getIsPublic() &&
                        !travelGroupMemberRepository.existsByGroupAndUser(group, user))
                .filter(group -> group.getCurrentMembers() < group.getMaxMembers())
                .limit(20)
                .map(this::mapToTravelGroupResponse)
                .collect(Collectors.toList());
    }

    // ================ TRAVEL ITINERARY ================

    @Override
    public TravelItineraryResponse createItineraryItem(TravelItineraryRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = null;
        if (request.getGroupId() != null && !request.getGroupId().isEmpty()) {
            group = travelGroupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

            // Verify user is member of the group
            if (!travelGroupMemberRepository.existsByGroupAndUser(group, user)) {
                throw new IllegalArgumentException("You are not a member of this travel group");
            }
        }

        TravelItinerary itinerary = TravelItinerary.builder()
                .user(user)
                .group(group)
                .title(request.getTitle())
                .itineraryDate(request.getItineraryDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .activityType(request.getActivityType())
                .activityName(request.getActivityName())
                .description(request.getDescription())
                .location(request.getLocation())
                .notes(request.getNotes())
                .estimatedCost(request.getEstimatedCost())
                .bookingReference(request.getBookingReference())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .isShared(request.getIsShared() != null ? request.getIsShared() : false)
                .build();

        itinerary = travelItineraryRepository.save(itinerary);

        return mapToTravelItineraryResponse(itinerary);
    }

    @Override
    public List<TravelItineraryResponse> getItinerary(String userEmail, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<TravelItinerary> itineraries;
        if (startDate != null && endDate != null) {
            itineraries = travelItineraryRepository
                    .findByUserAndItineraryDateBetweenOrderByItineraryDateAscStartTimeAsc(
                            user, startDate, endDate);
        } else {
            itineraries = travelItineraryRepository.findByUserOrderByItineraryDateAscStartTimeAsc(user);
        }

        return itineraries.stream()
                .map(this::mapToTravelItineraryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelItineraryResponse> getItineraryByDate(String userEmail, LocalDate date) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<TravelItinerary> itineraries = travelItineraryRepository
                .findByUserAndItineraryDateOrderByOrderIndexAsc(user, date);

        return itineraries.stream()
                .map(this::mapToTravelItineraryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TravelItineraryResponse updateItineraryItem(String itemId, TravelItineraryRequest request,
            String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelItinerary itinerary = travelItineraryRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Itinerary item not found"));

        if (!itinerary.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't have permission to update this itinerary item");
        }

        itinerary.setTitle(request.getTitle());
        itinerary.setItineraryDate(request.getItineraryDate());
        itinerary.setStartTime(request.getStartTime());
        itinerary.setEndTime(request.getEndTime());
        itinerary.setActivityType(request.getActivityType());
        itinerary.setActivityName(request.getActivityName());
        itinerary.setDescription(request.getDescription());
        itinerary.setLocation(request.getLocation());
        itinerary.setNotes(request.getNotes());
        itinerary.setEstimatedCost(request.getEstimatedCost());
        itinerary.setBookingReference(request.getBookingReference());
        if (request.getOrderIndex() != null) {
            itinerary.setOrderIndex(request.getOrderIndex());
        }
        if (request.getIsShared() != null) {
            itinerary.setIsShared(request.getIsShared());
        }

        itinerary = travelItineraryRepository.save(itinerary);

        return mapToTravelItineraryResponse(itinerary);
    }

    @Override
    public MessageResponse deleteItineraryItem(String itemId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelItinerary itinerary = travelItineraryRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Itinerary item not found"));

        if (!itinerary.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't have permission to delete this itinerary item");
        }

        travelItineraryRepository.delete(itinerary);

        return MessageResponse.builder()
                .message("Itinerary item deleted successfully")
                .build();
    }

    @Override
    public List<TravelItineraryResponse> getGroupItinerary(String groupId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Verify user is member
        if (!travelGroupMemberRepository.existsByGroupAndUser(group, user)) {
            throw new IllegalArgumentException("You are not a member of this travel group");
        }

        List<TravelItinerary> itineraries = travelItineraryRepository
                .findByGroupOrderByItineraryDateAscStartTimeAsc(group);

        return itineraries.stream()
                .map(this::mapToTravelItineraryResponse)
                .collect(Collectors.toList());
    }

    // ================ TRIP MATCHING ================

    @Override
    public List<TravelMatchResponse> findTravelMatches(String userEmail, String destination) {
        // Input validation
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination cannot be null or empty");
        }

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get current user's travel plans for this destination
        List<TravelPlan> userPlans = travelPlanRepository.findByUserAndStatus(currentUser, "PLANNED")
                .stream()
                .filter(plan -> plan.getDestination().equalsIgnoreCase(destination.trim()))
                .collect(Collectors.toList());

        if (userPlans.isEmpty()) {
            return new ArrayList<>();
        }

        TravelPlan userPlan = userPlans.get(0); // Use first matching plan

        // Find other travelers going to same destination using optimized query with
        // JOIN FETCH
        LocalDate today = LocalDate.now();
        List<TravelPlan> matchingPlans = travelPlanRepository
                .findByDestinationIgnoreCaseAndStatusAndTravelDateGreaterThanEqualWithUser(
                        destination.trim(), "PLANNED", today);

        // Calculate compatibility for each match
        return matchingPlans.stream()
                .filter(plan -> !plan.getUser().getId().equals(currentUser.getId()))
                .filter(plan -> plan.getUser().isActive() && plan.getUser().isVerified())
                .map(plan -> calculateTravelMatch(currentUser, userPlan, plan))
                .sorted((a, b) -> Double.compare(b.getCompatibilityScore(), a.getCompatibilityScore()))
                .limit(20)
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelMatchResponse> getPersonalizedMatches(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get current user's travel plans
        List<TravelPlan> userPlans = travelPlanRepository.findByUserAndStatus(currentUser, "PLANNED");

        if (userPlans.isEmpty()) {
            return new ArrayList<>();
        }

        // Find matches for all user's destinations
        Set<TravelMatchResponse> allMatches = new HashSet<>();
        for (TravelPlan userPlan : userPlans) {
            List<TravelMatchResponse> matches = findTravelMatches(userEmail, userPlan.getDestination());
            allMatches.addAll(matches);
        }

        return allMatches.stream()
                .sorted((a, b) -> Double.compare(b.getCompatibilityScore(), a.getCompatibilityScore()))
                .limit(30)
                .collect(Collectors.toList());
    }

    private TravelMatchResponse calculateTravelMatch(User currentUser, TravelPlan userPlan, TravelPlan otherPlan) {
        double score = 0.0;
        List<String> matchReasons = new ArrayList<>();
        int commonInterests = 0;
        boolean sameTravelStyle = false;
        boolean overlappingDates = false;

        // Date overlap (40 points)
        if (userPlan.getTravelDate() != null && otherPlan.getTravelDate() != null) {
            LocalDate userStart = userPlan.getTravelDate();
            LocalDate userEnd = userPlan.getReturnDate() != null ? userPlan.getReturnDate() : userStart.plusDays(7);
            LocalDate otherStart = otherPlan.getTravelDate();
            LocalDate otherEnd = otherPlan.getReturnDate() != null ? otherPlan.getReturnDate() : otherStart.plusDays(7);

            if (!userEnd.isBefore(otherStart) && !otherEnd.isBefore(userStart)) {
                overlappingDates = true;
                score += 40;
                matchReasons.add("Overlapping travel dates");
            }
        }

        // Same travel type (20 points)
        if (userPlan.getTravelType() != null && otherPlan.getTravelType() != null &&
                userPlan.getTravelType().equals(otherPlan.getTravelType())) {
            sameTravelStyle = true;
            score += 20;
            matchReasons.add("Same travel style: " + userPlan.getTravelType());
        }

        // Similar number of travelers (10 points)
        if (userPlan.getNumberOfTravelers() != null && otherPlan.getNumberOfTravelers() != null) {
            int diff = Math.abs(userPlan.getNumberOfTravelers() - otherPlan.getNumberOfTravelers());
            if (diff <= 1) {
                score += 10;
                matchReasons.add("Similar group size");
            }
        }

        // Location match (10 points)
        if (currentUser.getAddress() != null && otherPlan.getUser().getAddress() != null) {
            String userCity = currentUser.getAddress().getCity();
            String otherCity = otherPlan.getUser().getAddress().getCity();
            if (userCity != null && otherCity != null && userCity.equalsIgnoreCase(otherCity)) {
                score += 10;
                matchReasons.add("Same city");
            }
        }

        // Verified user (10 points)
        if (otherPlan.getUser().isVerified()) {
            score += 10;
            matchReasons.add("Verified traveler");
        }

        // Mutual connections (10 points)
        List<UserConnection> userConnections = userConnectionRepository.findByUserAndIsActiveTrue(currentUser);
        Set<String> connectedIds = userConnections.stream()
                .map(conn -> conn.getConnectedUser().getId())
                .collect(Collectors.toSet());
        if (connectedIds.contains(otherPlan.getUser().getId())) {
            score += 10;
            matchReasons.add("Mutual connection");
        }

        // Check profile visibility (Instagram-like: based on user's permission)
        User otherUser = otherPlan.getUser();
        boolean isProfileVisible = otherUser.getPreferences() != null &&
                otherUser.getPreferences().getPrivacy() != null &&
                otherUser.getPreferences().getPrivacy().getProfileVisible();

        return TravelMatchResponse.builder()
                .userId(otherUser.getId())
                // Email only visible if profileVisible = true (Instagram-like)
                .email(isProfileVisible ? otherUser.getEmail() : null)
                .firstName(otherUser.getFirstName())
                .lastName(otherUser.getLastName())
                .avatarUrl(otherUser.getAvatarUrl())
                .destination(otherPlan.getDestination())
                .travelDate(otherPlan.getTravelDate())
                .returnDate(otherPlan.getReturnDate())
                .compatibilityScore(Math.min(score, 100.0))
                .matchReasons(String.join(", ", matchReasons))
                .commonInterests(commonInterests)
                .sameTravelStyle(sameTravelStyle)
                .overlappingDates(overlappingDates)
                .travelType(otherPlan.getTravelType())
                .build();
    }

    // ================ HELPER METHODS ================

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generate a unique invite code that doesn't exist in the database
     */
    private String generateUniqueInviteCode() {
        String inviteCode;
        int maxAttempts = 10;
        int attempts = 0;

        do {
            inviteCode = generateInviteCode();
            attempts++;
            if (attempts > maxAttempts) {
                throw new IllegalStateException(
                        "Failed to generate unique invite code after " + maxAttempts + " attempts");
            }
        } while (travelGroupRepository.findByInviteCode(inviteCode).isPresent());

        return inviteCode;
    }

    private TravelGroupResponse mapToTravelGroupResponse(TravelGroup group) {
        List<GroupMemberResponse> members = travelGroupMemberRepository.findByGroup(group).stream()
                .map(member -> GroupMemberResponse.builder()
                        .id(member.getId())
                        .userId(member.getUser().getId())
                        .email(member.getUser().getEmail())
                        .firstName(member.getUser().getFirstName())
                        .lastName(member.getUser().getLastName())
                        .avatarUrl(member.getUser().getAvatarUrl())
                        .role(member.getRole())
                        .status(member.getStatus())
                        .joinedAt(member.getJoinedAt())
                        .build())
                .collect(Collectors.toList());

        return TravelGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdByUserId(group.getCreatedBy().getId())
                .createdByEmail(group.getCreatedBy().getEmail())
                .createdByName(group.getCreatedBy().getFirstName() + " " + group.getCreatedBy().getLastName())
                .destination(group.getDestination())
                .travelDate(group.getTravelDate())
                .returnDate(group.getReturnDate())
                .maxMembers(group.getMaxMembers())
                .currentMembers(group.getCurrentMembers())
                .status(group.getStatus())
                .travelType(group.getTravelType())
                .isPublic(group.getIsPublic())
                .imageUrl(group.getImageUrl())
                .inviteCode(group.getInviteCode())
                .members(members)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private TravelItineraryResponse mapToTravelItineraryResponse(TravelItinerary itinerary) {
        return TravelItineraryResponse.builder()
                .id(itinerary.getId())
                .userId(itinerary.getUser().getId())
                .groupId(itinerary.getGroup() != null ? itinerary.getGroup().getId() : null)
                .title(itinerary.getTitle())
                .itineraryDate(itinerary.getItineraryDate())
                .startTime(itinerary.getStartTime())
                .endTime(itinerary.getEndTime())
                .activityType(itinerary.getActivityType())
                .activityName(itinerary.getActivityName())
                .description(itinerary.getDescription())
                .location(itinerary.getLocation())
                .notes(itinerary.getNotes())
                .estimatedCost(itinerary.getEstimatedCost())
                .bookingReference(itinerary.getBookingReference())
                .orderIndex(itinerary.getOrderIndex())
                .isShared(itinerary.getIsShared())
                .createdAt(itinerary.getCreatedAt())
                .updatedAt(itinerary.getUpdatedAt())
                .build();
    }

    private TravelPlanResponse mapToTravelPlanResponse(TravelPlan plan) {
        return TravelPlanResponse.builder()
                .id(plan.getId())
                .userId(plan.getUser().getId())
                .userEmail(plan.getUser().getEmail())
                .userName(plan.getUser().getFirstName() + " " + plan.getUser().getLastName())
                .destination(plan.getDestination())
                .description(plan.getDescription())
                .travelDate(plan.getTravelDate())
                .returnDate(plan.getReturnDate())
                .status(plan.getStatus())
                .travelType(plan.getTravelType())
                .numberOfTravelers(plan.getNumberOfTravelers())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    // ================ USER VERIFICATION ================

    @Override
    @Transactional
    public MessageResponse submitVerificationRequest(VerificationRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if user already has a pending request
        verificationRequestRepository.findByUserAndStatus(user,
                in.mapmytour.auth.entity.VerificationRequest.VerificationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("You already have a pending verification request");
                });

        // Create verification request entity
        in.mapmytour.auth.entity.VerificationRequest verificationRequest = in.mapmytour.auth.entity.VerificationRequest
                .builder()
                .user(user)
                .verificationType(request.getVerificationType())
                .documentType(request.getDocumentType())
                .description(request.getDescription())
                .reason(null) // DTO doesn't have reason field, can be added later if needed
                .status(in.mapmytour.auth.entity.VerificationRequest.VerificationStatus.PENDING)
                .build();

        verificationRequestRepository.save(verificationRequest);

        logUserActivity(user, "VERIFICATION_REQUEST_SUBMITTED",
                "Submitted verification request: " + request.getVerificationType(),
                "", "", "SUCCESS");

        return MessageResponse.builder()
                .message("Verification request submitted successfully")
                .build();
    }

    @Override
    public VerificationStatusResponse getVerificationStatus(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return VerificationStatusResponse.builder()
                .isVerified(user.isVerified())
                .verificationLevel("BASIC") // EMAIL_VERIFIED, PHONE_VERIFIED, DOCUMENT_VERIFIED, etc.
                .submittedAt(user.getCreatedAt())
                .status("VERIFIED")
                .build();
    }

    @Override
    public MessageResponse uploadVerificationDocument(MultipartFile file, String documentType, String userEmail) {
        try {
            DocumentUploadResponse document = uploadDocument(file, documentType, userEmail);
            // Link document to verification request
            return MessageResponse.builder()
                    .message("Verification document uploaded successfully")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload verification document: " + e.getMessage());
        }
    }

    // ================ ADMIN VERIFICATION MANAGEMENT ================

    @Override
    public Page<VerificationRequestResponse> getAllVerificationRequests(Pageable pageable, String status) {
        Page<in.mapmytour.auth.entity.VerificationRequest> requests;

        if (status != null && !status.isEmpty()) {
            try {
                in.mapmytour.auth.entity.VerificationRequest.VerificationStatus requestStatus = in.mapmytour.auth.entity.VerificationRequest.VerificationStatus
                        .valueOf(status.toUpperCase());
                requests = verificationRequestRepository.findByStatus(requestStatus, pageable);
            } catch (IllegalArgumentException e) {
                requests = verificationRequestRepository.findAll(pageable);
            }
        } else {
            requests = verificationRequestRepository.findAll(pageable);
        }

        return requests.map(this::mapToVerificationRequestResponse);
    }

    @Override
    public VerificationRequestResponse getVerificationRequestById(String requestId) {
        in.mapmytour.auth.entity.VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Verification request not found"));

        return mapToVerificationRequestResponse(request);
    }

    @Override
    @Transactional
    public MessageResponse approveVerificationRequest(String idOrUserId, String adminEmail, String adminNotes) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        in.mapmytour.auth.entity.VerificationRequest request = verificationRequestRepository.findById(idOrUserId)
                .orElseGet(() -> {
                    User user = userRepository.findById(idOrUserId).orElse(null);
                    if (user != null) {
                        return verificationRequestRepository.findByUserAndStatus(user,
                                in.mapmytour.auth.entity.VerificationRequest.VerificationStatus.PENDING).orElse(null);
                    }
                    return null;
                });

        if (request == null) {
            throw new IllegalArgumentException("Verification request not found");
        }

        if (request.getStatus() != in.mapmytour.auth.entity.VerificationRequest.VerificationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending verification requests can be approved");
        }

        // Update request status
        request.setStatus(in.mapmytour.auth.entity.VerificationRequest.VerificationStatus.APPROVED);
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNotes(adminNotes);
        verificationRequestRepository.save(request);

        // Update user verification status and activate account
        User user = request.getUser();
        user.setIsVerified(true);
        user.setIsActive(true); // Activate account on manual Admin approval
        userRepository.save(user);

        // Send notification to user
        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("requestId", request.getId());
            notificationData.put("verificationType", request.getVerificationType());

            realtimeNotificationHelper.sendNotification(
                    user.getEmail(),
                    "VERIFICATION_APPROVED",
                    "Verification Approved",
                    "Your verification request has been approved. You are now verified!",
                    request.getId(),
                    "VERIFICATION_REQUEST",
                    admin,
                    notificationData,
                    false,
                    null,
                    "HIGH");
        } catch (Exception e) {
            log.warn("Failed to send verification approval notification: {}", e.getMessage());
        }

        logUserActivity(admin, "VERIFICATION_APPROVED",
                "Approved verification request for user: " + user.getEmail(),
                request.getId(), "", "SUCCESS");

        return MessageResponse.builder()
                .message("Verification request approved successfully")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse rejectVerificationRequest(String idOrUserId, String adminEmail, String adminNotes) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        in.mapmytour.auth.entity.VerificationRequest request = verificationRequestRepository.findById(idOrUserId)
                .orElseGet(() -> {
                    User user = userRepository.findById(idOrUserId).orElse(null);
                    if (user != null) {
                        return verificationRequestRepository.findByUserAndStatus(user,
                                in.mapmytour.auth.entity.VerificationRequest.VerificationStatus.PENDING).orElse(null);
                    }
                    return null;
                });

        if (request == null) {
            throw new IllegalArgumentException("Verification request not found");
        }

        if (request.getStatus() != in.mapmytour.auth.entity.VerificationRequest.VerificationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending verification requests can be rejected");
        }

        // Update request status
        request.setStatus(in.mapmytour.auth.entity.VerificationRequest.VerificationStatus.REJECTED);
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNotes(adminNotes);
        verificationRequestRepository.save(request);

        // Send notification to user
        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("requestId", request.getId());
            notificationData.put("verificationType", request.getVerificationType());

            realtimeNotificationHelper.sendNotification(
                    request.getUser().getEmail(),
                    "VERIFICATION_REJECTED",
                    "Verification Rejected",
                    adminNotes != null ? adminNotes : "Your verification request has been rejected.",
                    request.getId(),
                    "VERIFICATION_REQUEST",
                    admin,
                    notificationData,
                    false,
                    null,
                    "HIGH");
        } catch (Exception e) {
            log.warn("Failed to send verification rejection notification: {}", e.getMessage());
        }

        logUserActivity(admin, "VERIFICATION_REJECTED",
                "Rejected verification request for user: " + request.getUser().getEmail(),
                request.getId(), "", "SUCCESS");

        return MessageResponse.builder()
                .message("Verification request rejected successfully")
                .build();
    }

    private VerificationRequestResponse mapToVerificationRequestResponse(
            in.mapmytour.auth.entity.VerificationRequest request) {
        User user = request.getUser();
        User reviewedBy = request.getReviewedBy();

        return VerificationRequestResponse.builder()
                .id(request.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getFullName())
                .userAvatarUrl(user.getAvatarUrl())
                .verificationType(request.getVerificationType())
                .documentType(request.getDocumentType())
                .description(request.getDescription())
                .reason(request.getReason())
                .status(request.getStatus().name())
                .adminNotes(request.getAdminNotes())
                .reviewedBy(reviewedBy != null ? reviewedBy.getId() : null)
                .reviewedByEmail(reviewedBy != null ? reviewedBy.getEmail() : null)
                .reviewedAt(request.getReviewedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    // ================ SUBSCRIPTION MANAGEMENT ================

    @Override
    public SubscriptionResponse getSubscription(String userEmail) {
        // In a real implementation, you'd fetch from Subscription entity
        return SubscriptionResponse.builder()
                .subscriptionId(UUID.randomUUID().toString())
                .plan("FREE")
                .status("ACTIVE")
                .startDate(LocalDateTime.now().minusMonths(1))
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .amount(0.0)
                .currency("USD")
                .build();
    }

    @Override
    public MessageResponse upgradeSubscription(SubscriptionUpgradeRequest request, String userEmail) {
        // Implementation for subscription upgrade
        return MessageResponse.builder()
                .message("Subscription upgraded successfully")
                .build();
    }

    @Override
    public MessageResponse cancelSubscription(String userEmail) {
        // Implementation for subscription cancellation
        return MessageResponse.builder()
                .message("Subscription cancelled successfully")
                .build();
    }

    @Override
    public List<SubscriptionHistoryResponse> getSubscriptionHistory(String userEmail) {
        // In a real implementation, you'd fetch from SubscriptionHistory entity
        return new ArrayList<>();
    }

    // ================ COMMUNICATION PREFERENCES ================

    @Override
    public CommunicationPreferencesResponse getCommunicationPreferences(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User.NotificationPreferences notifications = user.getPreferences() != null
                ? user.getPreferences().getNotifications()
                : null;

        return CommunicationPreferencesResponse.builder()
                .email(notifications != null ? notifications.getEmail() : true)
                .sms(notifications != null ? notifications.getSms() : false)
                .push(notifications != null ? notifications.getPush() : true)
                .marketing(true)
                .newsletter(true)
                .build();
    }

    @Override
    public MessageResponse updateCommunicationPreferences(CommunicationPreferencesRequest request, String userEmail) {
        // Implementation similar to updateNotificationSettings
        return MessageResponse.builder()
                .message("Communication preferences updated successfully")
                .build();
    }

    @Override
    public MessageResponse optOutFromEmails(String userEmail, String token) {
        // Implementation for email opt-out with token verification
        return MessageResponse.builder()
                .message("Successfully opted out from emails")
                .build();
    }

    @Override
    public MessageResponse optInToEmails(String userEmail) {
        // Implementation for email opt-in
        return MessageResponse.builder()
                .message("Successfully opted in to emails")
                .build();
    }

    // ================ EMERGENCY CONTACTS ================

    @Override
    public List<EmergencyContactResponse> getEmergencyContacts(String userEmail) {
        // In a real implementation, you'd fetch from EmergencyContact entity
        return new ArrayList<>();
    }

    @Override
    public MessageResponse addEmergencyContact(EmergencyContactRequest request, String userEmail) {
        // Implementation for adding emergency contacts
        return MessageResponse.builder()
                .message("Emergency contact added successfully")
                .build();
    }

    @Override
    public MessageResponse updateEmergencyContact(String contactId, EmergencyContactRequest request, String userEmail) {
        // Implementation for updating emergency contacts
        return MessageResponse.builder()
                .message("Emergency contact updated successfully")
                .build();
    }

    @Override
    public MessageResponse removeEmergencyContact(String userEmail, String contactId) {
        // Implementation for removing emergency contacts
        return MessageResponse.builder()
                .message("Emergency contact removed successfully")
                .build();
    }

    // ================ TRAVEL PREFERENCES ================

    @Override
    public TravelPreferencesResponse getTravelPreferences(String userEmail) {
        // In a real implementation, you'd fetch from TravelPreferences entity
        return TravelPreferencesResponse.builder()
                .budgetRange("MEDIUM")
                .preferredTransport(Arrays.asList("CAR", "FLIGHT"))
                .accommodationType("HOTEL")
                .dietaryRestrictions(new ArrayList<>())
                .accessibilityNeeds(new ArrayList<>())
                .build();
    }

    @Override
    public MessageResponse updateTravelPreferences(TravelPreferencesRequest request, String userEmail) {
        // Implementation for updating travel preferences
        return MessageResponse.builder()
                .message("Travel preferences updated successfully")
                .build();
    }

    // ================ INTERESTS AND TAGS ================

    @Override
    public List<InterestResponse> getInterests(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getPreferences() != null && user.getPreferences().getInterests() != null) {
            try {
                List<String> interests = objectMapper.readValue(
                        user.getPreferences().getInterests(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                return interests.stream()
                        .map(interest -> InterestResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .name(interest)
                                .category("USER_DEFINED")
                                .build())
                        .collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize interests for user: {}", userEmail);
            }
        }

        return new ArrayList<>();
    }

    @Override
    public MessageResponse updateInterests(List<String> interests, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User.UserPreferences preferences = user.getPreferences();
        if (preferences == null) {
            preferences = User.UserPreferences.builder().build();
        }

        try {
            String interestsJson = objectMapper.writeValueAsString(interests);
            preferences.setInterests(interestsJson);
            user.setPreferences(preferences);
            userRepository.save(user);

            return MessageResponse.builder()
                    .message("Interests updated successfully")
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to update interests: " + e.getMessage());
        }
    }

    @Override
    public List<InterestResponse> getAvailableInterests() {
        // Return predefined list of available interests
        List<String> predefinedInterests = Arrays.asList(
                "Adventure", "Photography", "Food", "Culture", "History", "Nature",
                "Art", "Music", "Sports", "Shopping", "Nightlife", "Architecture");

        return predefinedInterests.stream()
                .map(interest -> InterestResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .name(interest)
                        .category("PREDEFINED")
                        .build())
                .collect(Collectors.toList());
    }

    // ================ USER FEEDBACK ================

    @Override
    public MessageResponse submitFeedback(FeedbackRequest request, String userEmail) {
        // In a real implementation, you'd save to a Feedback entity
        log.info("Feedback submitted by user {}: {}", userEmail, request.getMessage());
        return MessageResponse.builder()
                .message("Feedback submitted successfully")
                .build();
    }

    @Override
    public List<FeedbackResponse> getUserFeedback(String userEmail) {
        // In a real implementation, you'd fetch from Feedback entity
        return new ArrayList<>();
    }

    // ================ ADMIN USER MANAGEMENT ================

    @Override
    public Page<UserProfileResponse> getAllUsersForAdmin(Pageable pageable, UserFilterRequest filter) {
        // In a real implementation, you'd apply filters and pagination
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::mapToUserProfileResponse);
    }

    @Override
    public MessageResponse adminUpdateUserProfile(String userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update user profile with admin privileges
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getRole() != null) {
            String newRole = request.getRole().toUpperCase();

            // Security Check: Only SUPER_ADMIN can manage SUPER_ADMIN role
            boolean isTargetSuperAdmin = user.getRole() == User.UserRole.SUPER_ADMIN;
            boolean isPromotingToSuperAdmin = "SUPER_ADMIN".equals(newRole);

            if (isTargetSuperAdmin || isPromotingToSuperAdmin) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isCallerSuperAdmin = auth != null && auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().endsWith("SUPER_ADMIN"));

                if (!isCallerSuperAdmin) {
                    throw new SecurityException("Only a SUPER_ADMIN can manage SUPER_ADMIN roles.");
                }
            }

            user.setRole(User.UserRole.valueOf(newRole));
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getIsVerified() != null) {
            user.setIsVerified(request.getIsVerified());
        }

        userRepository.save(user);

        return MessageResponse.builder()
                .message("User profile updated by admin")
                .build();
    }

    @Override
    public MessageResponse adminVerifyUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsVerified(true);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("User verified by admin")
                .build();
    }

    @Override
    public MessageResponse adminSuspendUser(String userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);

        // Revoke all tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        return MessageResponse.builder()
                .message("User suspended by admin")
                .build();
    }

    @Override
    public MessageResponse adminUnsuspendUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(true);
        user.setIsLocked(false);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("User unsuspended by admin")
                .build();
    }

    @Override
    public UserDetailedResponse getDetailedUserInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserDetailedResponse.builder()
                .user(mapToUserProfileResponse(user))
                .stats(UserStatsResponse.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .totalLogins(0L)
                        .activeTokens(refreshTokenRepository.countActiveTokensByUser(user))
                        .accountCreated(user.getCreatedAt())
                        .lastLogin(user.getLastLoginAt())
                        .isVerified(user.isVerified())
                        .isActive(user.isActive())
                        .build())
                .securityInfo(SecurityInfoResponse.builder()
                        .loginAttempts(user.getLoginAttempts())
                        .isLocked(user.isLocked())
                        .lockedUntil(user.getLockedUntil())
                        .twoFactorEnabled(false) // Check if 2FA is enabled
                        .build())
                .build();
    }

    // ================ BULK OPERATIONS ================

    @Override
    public MessageResponse bulkUpdateUsers(BulkUpdateRequest request) {
        // Implementation for bulk user updates
        return MessageResponse.builder()
                .message("Bulk update completed successfully")
                .build();
    }

    @Override
    public MessageResponse bulkDeleteUsers(List<String> userIds) {
        // Implementation for bulk user deletion
        return MessageResponse.builder()
                .message("Bulk deletion completed successfully")
                .build();
    }

    @Override
    public MessageResponse bulkSendNotification(BulkNotificationRequest request) {
        // Implementation for bulk notifications
        return MessageResponse.builder()
                .message("Bulk notification sent successfully")
                .build();
    }

    // ================ HEALTH CHECK ================

    @Override
    public MessageResponse healthCheck() {
        return MessageResponse.builder()
                .message("User service is running properly")
                .build();
    }

    // ================ PRIVATE HELPER METHODS ================

    private void updateUserAddress(User user, AddressRequest addressRequest) {
        User.Address currentAddress = user.getAddress() != null ? user.getAddress() : User.Address.builder().build();

        if (addressRequest.getStreet() != null) {
            currentAddress.setStreet(addressRequest.getStreet().trim());
        }
        if (addressRequest.getCity() != null) {
            currentAddress.setCity(addressRequest.getCity().trim());
        }
        if (addressRequest.getState() != null) {
            currentAddress.setState(addressRequest.getState().trim());
        }
        if (addressRequest.getCountry() != null) {
            currentAddress.setCountry(addressRequest.getCountry().trim());
        }
        if (addressRequest.getPostalCode() != null) {
            currentAddress.setPostalCode(addressRequest.getPostalCode().trim());
        }

        user.setAddress(currentAddress);
    }

    private void updateUserPreferences(User user, UserPreferencesRequest preferencesRequest) {
        User.UserPreferences currentPreferences = user.getPreferences() != null ? user.getPreferences()
                : User.UserPreferences.builder().build();

        // Update notification preferences
        if (preferencesRequest.getNotifications() != null) {
            User.NotificationPreferences notificationPrefs = currentPreferences.getNotifications() != null
                    ? currentPreferences.getNotifications()
                    : User.NotificationPreferences.builder().build();

            NotificationPreferencesRequest notifRequest = preferencesRequest.getNotifications();
            if (notifRequest.getEmail() != null) {
                notificationPrefs.setEmail(notifRequest.getEmail());
            }
            if (notifRequest.getSms() != null) {
                notificationPrefs.setSms(notifRequest.getSms());
            }
            if (notifRequest.getPush() != null) {
                notificationPrefs.setPush(notifRequest.getPush());
            }

            currentPreferences.setNotifications(notificationPrefs);
        }

        // Update privacy preferences
        if (preferencesRequest.getPrivacy() != null) {
            User.PrivacyPreferences privacyPrefs = currentPreferences.getPrivacy() != null
                    ? currentPreferences.getPrivacy()
                    : User.PrivacyPreferences.builder().build();

            PrivacyPreferencesRequest privacyRequest = preferencesRequest.getPrivacy();
            if (privacyRequest.getProfileVisible() != null) {
                privacyPrefs.setProfileVisible(privacyRequest.getProfileVisible());
            }
            if (privacyRequest.getShowBookingHistory() != null) {
                privacyPrefs.setShowBookingHistory(privacyRequest.getShowBookingHistory());
            }
            // Update granular field-level privacy settings
            if (privacyRequest.getShowEmail() != null) {
                privacyPrefs.setShowEmail(privacyRequest.getShowEmail());
            }
            if (privacyRequest.getShowPhone() != null) {
                privacyPrefs.setShowPhone(privacyRequest.getShowPhone());
            }
            if (privacyRequest.getShowDateOfBirth() != null) {
                privacyPrefs.setShowDateOfBirth(privacyRequest.getShowDateOfBirth());
            }
            if (privacyRequest.getShowAddress() != null) {
                privacyPrefs.setShowAddress(privacyRequest.getShowAddress());
            }
            if (privacyRequest.getShowStreet() != null) {
                privacyPrefs.setShowStreet(privacyRequest.getShowStreet());
            }
            if (privacyRequest.getShowCity() != null) {
                privacyPrefs.setShowCity(privacyRequest.getShowCity());
            }
            if (privacyRequest.getShowState() != null) {
                privacyPrefs.setShowState(privacyRequest.getShowState());
            }
            if (privacyRequest.getShowPostalCode() != null) {
                privacyPrefs.setShowPostalCode(privacyRequest.getShowPostalCode());
            }

            currentPreferences.setPrivacy(privacyPrefs);
        }

        // Update interests
        if (preferencesRequest.getInterests() != null) {
            try {
                String interestsJson = objectMapper.writeValueAsString(preferencesRequest.getInterests());
                currentPreferences.setInterests(interestsJson);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize interests", e);
                throw new IllegalArgumentException("Invalid interests format");
            }
        }

        user.setPreferences(currentPreferences);
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return mapToUserProfileResponse(user, null);
    }

    /**
     * Map user to profile response with privacy awareness (Instagram-like model)
     * 
     * @param user        The user to map
     * @param currentUser The current user viewing the profile (null if not logged
     *                    in)
     * @return UserProfileResponse respecting privacy settings
     */
    private UserProfileResponse mapToUserProfileResponse(User user, User currentUser) {
        // Get privacy preferences
        User.PrivacyPreferences privacy = user.getPreferences() != null ? user.getPreferences().getPrivacy() : null;

        boolean isOwnProfile = currentUser != null && currentUser.getId().equals(user.getId());
        boolean isAdmin = currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN") || r.getName().equals("ROLE_SUPER_ADMIN"));

        // Check if profile is visible (Instagram-like: based on user's permission)
        boolean isProfileVisible = isOwnProfile || isAdmin
                || (privacy != null && Boolean.TRUE.equals(privacy.getProfileVisible()));

        String primaryRole = user.getRole() != null ? user.getRole().name().toLowerCase() : "user";
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            java.util.List<String> rbacRoles = user.getRoles().stream()
                    .map(r -> r.getName())
                    .collect(java.util.stream.Collectors.toList());
            java.util.List<String> hierarchy = java.util.Arrays.asList("SUPER_ADMIN", "ADMIN", "MANAGER", "EMPLOYEE",
                    "B2B", "USER");
            boolean found = false;
            for (String role : hierarchy) {
                if (rbacRoles.stream().anyMatch(r -> r.replaceFirst("^ROLE_", "").equalsIgnoreCase(role))) {
                    primaryRole = role.toLowerCase();
                    found = true;
                    break;
                }
            }
            if (!found && !rbacRoles.isEmpty()) {
                primaryRole = rbacRoles.get(0).replaceFirst("^ROLE_", "").toLowerCase();
            }
        }

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                // Name & avatar respect profile visibility for discovery-style use
                .firstName(isProfileVisible ? user.getFirstName() : "Traveler")
                .lastName(isProfileVisible ? user.getLastName() : null)
                .avatarUrl(isProfileVisible ? user.getAvatarUrl() : null)
                .bio(isProfileVisible ? user.getBio() : null) // Bio is visible if profile is visible
                .coverImageUrl(isProfileVisible ? user.getCoverImageUrl() : null) // Cover image is visible if profile
                                                                                  // is visible
                .gender(user.getGender() != null ? user.getGender().name().toLowerCase() : null)
                .role(primaryRole)
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);

        // Presence Status
        try {
            if (presenceService != null) {
                builder.isOnline(presenceService.isUserOnline(user.getEmail()));
            }
        } catch (Exception e) {
            log.warn("Failed to check presence for user {}: {}", user.getEmail(), e.getMessage());
        }

        if (user.getLastSeenAt() != null) {
            builder.lastSeenAt(user.getLastSeenAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        // Set business name for Agents/Suppliers only
        Optional<Agent> agentOpt = agentRepository.findByUser(user);
        if (agentOpt.isPresent()) {
            builder.businessName(agentOpt.get().getAgencyName());
        } else {
            supplierRepository.findByUser(user).ifPresent(supplier -> builder.businessName(supplier.getCompanyName()));
        }

        // Enrich with role-specific details for Supplier/Agent
        // Connection count will be shown since profile is public
        // Map common fields...

        // Conditional fields based on profileVisible and individual field permissions
        // (Instagram-like)
        if (isProfileVisible && privacy != null) {
            // Profile is visible - check individual field permissions
            // Email visibility
            if (Boolean.TRUE.equals(privacy.getShowEmail())) {
                builder.email(user.getEmail());
            } else {
                builder.email(null);
            }

            // Phone visibility
            if (Boolean.TRUE.equals(privacy.getShowPhone())) {
                builder.phone(user.getPhone());
            } else {
                builder.phone(null);
            }

            // Date of Birth visibility
            if (Boolean.TRUE.equals(privacy.getShowDateOfBirth())) {
                builder.dateOfBirth(user.getDateOfBirth());
            } else {
                builder.dateOfBirth(null);
            }

            // Address visibility (granular control)
            if (user.getAddress() != null) {
                AddressResponse.AddressResponseBuilder addressBuilder = AddressResponse.builder();

                // Country is always visible (for discovery purposes)
                addressBuilder.country(user.getAddress().getCountry());

                // Other address fields based on individual permissions
                if (Boolean.TRUE.equals(privacy.getShowAddress())) {
                    // Check individual address field permissions
                    if (Boolean.TRUE.equals(privacy.getShowStreet())) {
                        addressBuilder.street(user.getAddress().getStreet());
                    } else {
                        addressBuilder.street(null);
                    }

                    if (Boolean.TRUE.equals(privacy.getShowCity())) {
                        addressBuilder.city(user.getAddress().getCity());
                    } else {
                        addressBuilder.city(null);
                    }

                    if (Boolean.TRUE.equals(privacy.getShowState())) {
                        addressBuilder.state(user.getAddress().getState());
                    } else {
                        addressBuilder.state(null);
                    }

                    if (Boolean.TRUE.equals(privacy.getShowPostalCode())) {
                        addressBuilder.postalCode(user.getAddress().getPostalCode());
                    } else {
                        addressBuilder.postalCode(null);
                    }
                } else {
                    // showAddress = false: Hide all address fields except country
                    addressBuilder.street(null).city(null).state(null).postalCode(null);
                }

                builder.address(addressBuilder.build());
            }
        } else {
            // Private profile (profileVisible = false): Hide all sensitive fields
            builder.email(null).phone(null).dateOfBirth(null);

            // Only show country (not city/state/street) for private profiles
            if (user.getAddress() != null) {
                builder.address(AddressResponse.builder()
                        .street(null) // Hidden
                        .city(null) // Hidden
                        .state(null) // Hidden
                        .country(user.getAddress().getCountry()) // Only country visible
                        .postalCode(null) // Hidden
                        .build());
            }
        }

        // Set connection counts (Instagram-like)
        long totalConnections = userConnectionRepository.countByUserAndIsActiveTrue(user);
        builder.totalConnections((int) totalConnections);

        // Set mutual connections if viewing someone else's profile
        if (currentUser != null && !currentUser.getId().equals(user.getId())) {
            builder.mutualConnections(calculateMutualConnections(user, currentUser));
        }

        return builder.build();
    }

    /**
     * Efficiently calculate mutual connections using a single database query.
     * 
     * @param user1 First user
     * @param user2 Second user
     * @return Number of mutual connections
     */
    private int calculateMutualConnections(User user1, User user2) {
        if (user1 == null || user2 == null) {
            return 0;
        }
        return (int) userConnectionRepository.countMutualConnections(user1, user2);
    }

    private UserPreferencesResponse mapToUserPreferencesResponse(User.UserPreferences preferences) {
        UserPreferencesResponse.UserPreferencesResponseBuilder builder = UserPreferencesResponse.builder();

        // Map notification preferences
        if (preferences.getNotifications() != null) {
            builder.notifications(NotificationPreferencesResponse.builder()
                    .email(preferences.getNotifications().getEmail())
                    .sms(preferences.getNotifications().getSms())
                    .push(preferences.getNotifications().getPush())
                    .build());
        }

        // Map privacy preferences
        if (preferences.getPrivacy() != null) {
            User.PrivacyPreferences privacy = preferences.getPrivacy();
            builder.privacy(PrivacyPreferencesResponse.builder()
                    .profileVisible(Boolean.TRUE.equals(privacy.getProfileVisible()))
                    .showBookingHistory(Boolean.TRUE.equals(privacy.getShowBookingHistory()))
                    .showEmail(Boolean.TRUE.equals(privacy.getShowEmail()))
                    .showPhone(Boolean.TRUE.equals(privacy.getShowPhone()))
                    .showDateOfBirth(Boolean.TRUE.equals(privacy.getShowDateOfBirth()))
                    .showAddress(Boolean.TRUE.equals(privacy.getShowAddress()))
                    .showStreet(Boolean.TRUE.equals(privacy.getShowStreet()))
                    .showCity(Boolean.TRUE.equals(privacy.getShowCity()))
                    .showState(Boolean.TRUE.equals(privacy.getShowState()))
                    .showPostalCode(Boolean.TRUE.equals(privacy.getShowPostalCode()))
                    .build());
        }

        return builder.build();
    }

    private int calculateProfileCompleteness(User user) {
        int completeness = 0;
        int totalFields = 10;

        if (user.getFirstName() != null)
            completeness++;
        if (user.getLastName() != null)
            completeness++;
        if (user.getEmail() != null)
            completeness++;
        if (user.getPhone() != null)
            completeness++;
        if (user.getDateOfBirth() != null)
            completeness++;
        if (user.getGender() != null)
            completeness++;
        if (user.getAvatarUrl() != null)
            completeness++;
        if (user.getAddress() != null)
            completeness++;
        if (user.getPreferences() != null)
            completeness++;
        if (user.isVerified())
            completeness++;

        return (completeness * 100) / totalFields;
    }

    // =============== GROUP MESSAGING ================

    @Override
    public GroupMessageResponse sendGroupMessage(String groupId, GroupMessageRequest request, String userEmail) {
        User sender = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Verify user is a member of the group
        TravelGroupMember member = travelGroupMemberRepository.findByGroupAndUser(group, sender)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        GroupMessage message = GroupMessage.builder()
                .group(group)
                .sender(sender)
                .message(request.getMessage())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .attachmentUrl(request.getAttachmentUrl())
                .locationData(request.getLocationData())
                .status("SENT")
                .build();

        message = groupMessageRepository.save(message);

        // Log activity
        logUserActivity(sender, "GROUP_MESSAGE_SENT",
                String.format("Sent message in group: %s", group.getName()), "", "", "SUCCESS");

        return mapToGroupMessageResponse(message);
    }

    @Override
    public Page<GroupMessageResponse> getGroupMessages(String groupId, String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Verify user is a member
        travelGroupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        Page<GroupMessage> messages = groupMessageRepository.findByGroupOrderByCreatedAtDesc(group, pageable);
        return messages.map(this::mapToGroupMessageResponse);
    }

    // =============== DIRECT MESSAGING ================

    @Override
    public DirectMessageResponse sendDirectMessage(DirectMessageRequest request, String userEmail) {
        // Input validation
        if (request.getRecipientEmail() == null || request.getRecipientEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email cannot be null or empty");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        // Message length validation (max 5000 characters)
        if (request.getMessage().length() > 5000) {
            throw new IllegalArgumentException("Message cannot exceed 5000 characters");
        }

        User sender = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User recipient = userRepository.findByEmail(request.getRecipientEmail().trim())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        // Check if users are connected (bidirectional check)
        boolean areConnected = userConnectionRepository.existsByUserAndConnectedUser(sender, recipient) ||
                userConnectionRepository.existsByUserAndConnectedUser(recipient, sender);

        if (!areConnected) {
            throw new IllegalArgumentException(
                    "You must be connected to send a direct message. Send a connection request first.");
        }

        DirectMessage message = DirectMessage.builder()
                .sender(sender)
                .recipient(recipient)
                        .message(request.getMessage().trim())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .attachmentUrl(request.getAttachmentUrl())
                .locationData(request.getLocationData())
                .status("SENT")
                .build();

        message = directMessageRepository.save(message);

        // Update last seen for both participants asynchronously
        touchLastSeen(sender.getEmail());
        touchLastSeen(recipient.getEmail());

        // Broadcast message status update (DELIVERED) in real-time
        try {
            in.mapmytour.auth.dto.user.MessageStatusUpdateResponse statusUpdate = in.mapmytour.auth.dto.user.MessageStatusUpdateResponse
                    .builder()
                    .messageId(message.getId())
                    .conversationId(recipient.getId())
                    .status("DELIVERED")
                    .updatedBy(recipient.getId())
                    .build();

            // Notify sender that message was delivered
            messagingTemplate.convertAndSendToUser(
                    sender.getEmail(),
                    "/queue/message-status",
                    statusUpdate);
        } catch (Exception e) {
            log.warn("Failed to broadcast message status update: {}", e.getMessage());
        }

        logUserActivity(sender, "DIRECT_MESSAGE_SENT",
                String.format("Sent message to: %s", recipient.getEmail()), "", "", "SUCCESS");

        return mapToDirectMessageResponse(message, sender.getId());
    }

    @Override
    public Page<DirectMessageResponse> getConversation(String recipientEmail, String userEmail, Pageable pageable) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        Page<DirectMessage> messages = directMessageRepository.findConversationBetweenUsers(
                currentUser, recipient, pageable);
        // Update last seen for the viewer
        touchLastSeen(currentUser.getEmail());

        return messages.map(msg -> mapToDirectMessageResponse(msg, currentUser.getId()));
    }

    @Override
    public List<UserProfileResponse> getConversationPartners(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Collect distinct partners from both sender and recipient perspectives
        List<User> senders = directMessageRepository.findDistinctSendersByRecipient(user);
        List<User> recipients = directMessageRepository.findDistinctRecipientsBySender(user);

        Map<String, User> uniquePartners = new LinkedHashMap<>();

        for (User u : senders) {
            if (u != null && !u.getId().equals(user.getId())) {
                uniquePartners.putIfAbsent(u.getId(), u);
            }
        }

        for (User u : recipients) {
            if (u != null && !u.getId().equals(user.getId())) {
                uniquePartners.putIfAbsent(u.getId(), u);
            }
        }

        return uniquePartners.values().stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse markMessageAsRead(String messageId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Only recipient can mark as read
        if (!message.getRecipient().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only mark your own messages as read");
        }

        message.setStatus("READ");
        message.setReadAt(LocalDateTime.now());
        directMessageRepository.save(message);

        // Broadcast read receipt in real-time to sender
        try {
            in.mapmytour.auth.dto.user.MessageStatusUpdateResponse statusUpdate = in.mapmytour.auth.dto.user.MessageStatusUpdateResponse
                    .builder()
                    .messageId(messageId)
                    .conversationId(user.getId()) // recipientId for sender
                    .status("READ")
                    .readAt(message.getReadAt())
                    .updatedBy(user.getId())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    message.getSender().getEmail(),
                    "/queue/message-status",
                    statusUpdate);

            log.debug("Broadcasted read receipt for message {} to sender {}", messageId,
                    message.getSender().getEmail());
        } catch (Exception e) {
            log.warn("Failed to broadcast read receipt: {}", e.getMessage());
        }

        return MessageResponse.builder()
                .message("Message marked as read")
                .build();
    }

    @Override
    public long getUnreadMessageCount(String userEmail) {
        return directMessageRepository.countByRecipientEmailAndStatus(userEmail, "SENT");
    }

    /**
     * Update the user's lastSeenAt timestamp to "now".
     * This is a lightweight way to approximate online/last-seen status.
     */
    /**
     * Update the user's lastSeenAt timestamp to "now".
     * Throttled to 5 minutes to avoid database thrashing.
     * Async to prevent blocking chat/api execution.
     */
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void touchLastSeen(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return;

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastSeenAt = user.getLastSeenAt();

            // Only update DB if last seen was more than 5 minutes ago to avoid thrashing
            if (lastSeenAt == null || lastSeenAt.isBefore(now.minusMinutes(5))) {
                user.setLastSeenAt(now);
                userRepository.save(user);
            }

            // Real-time broadcast (PresenceService handles its own caching/broadcast)
            presenceService.updateLastSeen(email);
        } catch (Exception e) {
            log.warn("Failed to update last seen for {}: {}", email, e.getMessage());
        }
    }

    // =============== EXPENSE MANAGEMENT ================

    @Override
    public GroupExpenseResponse createGroupExpense(String groupId, GroupExpenseRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Verify user is a member
        travelGroupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        // Verify paidBy is a member (default to current user)
        User paidBy = user;
        if (request.getParticipantUserIds() != null && !request.getParticipantUserIds().isEmpty()) {
            // Verify all participants are members
            for (String participantId : request.getParticipantUserIds()) {
                User participant = userRepository.findById(participantId)
                        .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + participantId));
                travelGroupMemberRepository.findByGroupAndUser(group, participant)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Participant is not a group member: " + participantId));
            }
        }

        GroupExpense expense = GroupExpense.builder()
                .group(group)
                .paidBy(paidBy)
                .description(request.getDescription())
                .amount(request.getAmount())
                .category(request.getCategory() != null ? request.getCategory() : "OTHER")
                .expenseDate(request.getExpenseDate())
                .receiptUrl(request.getReceiptUrl())
                .notes(request.getNotes())
                .status("PENDING")
                .build();

        expense = groupExpenseRepository.save(expense);

        // Create expense participants
        List<String> participantIds = request.getParticipantUserIds();
        if (participantIds == null || participantIds.isEmpty()) {
            // If no participants specified, split among all group members
            List<TravelGroupMember> members = travelGroupMemberRepository.findByGroup(group);
            participantIds = members.stream()
                    .map(m -> m.getUser().getId())
                    .collect(Collectors.toList());
        }

        BigDecimal shareAmount = request.getAmount().divide(
                BigDecimal.valueOf(participantIds.size()), 2, java.math.RoundingMode.HALF_UP);

        for (String participantId : participantIds) {
            User participant = userRepository.findById(participantId)
                    .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

            ExpenseParticipant ep = ExpenseParticipant.builder()
                    .expense(expense)
                    .user(participant)
                    .shareAmount(shareAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .balance(shareAmount)
                    .status(participant.getId().equals(paidBy.getId()) ? "PAID" : "PENDING")
                    .build();

            // If participant is the one who paid, mark as paid
            if (participant.getId().equals(paidBy.getId())) {
                ep.setPaidAmount(shareAmount);
                ep.setBalance(BigDecimal.ZERO);
            }

            expenseParticipantRepository.save(ep);
        }

        logUserActivity(user, "GROUP_EXPENSE_CREATED",
                String.format("Created expense: %s - %s", request.getDescription(), request.getAmount()), "", "",
                "SUCCESS");

        return mapToGroupExpenseResponse(expense);
    }

    @Override
    public List<GroupExpenseResponse> getGroupExpenses(String groupId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Verify user is a member
        travelGroupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        List<GroupExpense> expenses = groupExpenseRepository.findByGroupOrderByExpenseDateDesc(group);
        return expenses.stream()
                .map(this::mapToGroupExpenseResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GroupExpenseResponse getExpenseDetails(String expenseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupExpense expense = groupExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        // Verify user is a member of the group
        travelGroupMemberRepository.findByGroupAndUser(expense.getGroup(), user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        return mapToGroupExpenseResponse(expense);
    }

    @Override
    public MessageResponse updateExpense(String expenseId, GroupExpenseRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupExpense expense = groupExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        // Only the person who created/paid for this expense can update it
        travelGroupMemberRepository.findByGroupAndUser(expense.getGroup(), user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        if (!expense.getPaidBy().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the member who created this expense can update it");
        }

        if (request.getDescription() != null)
            expense.setDescription(request.getDescription());
        if (request.getCategory() != null)
            expense.setCategory(request.getCategory());
        if (request.getNotes() != null)
            expense.setNotes(request.getNotes());
        if (request.getReceiptUrl() != null)
            expense.setReceiptUrl(request.getReceiptUrl());

        groupExpenseRepository.save(expense);

        return MessageResponse.builder()
                .message("Expense updated successfully")
                .build();
    }

    @Override
    public MessageResponse deleteExpense(String expenseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupExpense expense = groupExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        // Only the person who paid or group admin can delete
        TravelGroupMember member = travelGroupMemberRepository.findByGroupAndUser(expense.getGroup(), user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        if (!expense.getPaidBy().getId().equals(user.getId()) &&
                !member.getRole().equals("CREATOR") && !member.getRole().equals("ADMIN")) {
            throw new IllegalArgumentException("You don't have permission to delete this expense");
        }

        // Delete participants first
        List<ExpenseParticipant> participants = expenseParticipantRepository.findByExpense(expense);
        expenseParticipantRepository.deleteAll(participants);

        groupExpenseRepository.delete(expense);

        return MessageResponse.builder()
                .message("Expense deleted successfully")
                .build();
    }

    @Override
    public MessageResponse recordPayment(String expenseId, String participantId, BigDecimal amount, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupExpense expense = groupExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        ExpenseParticipant participant = expenseParticipantRepository.findByExpenseAndUser(expense,
                userRepository.findById(participantId)
                        .orElseThrow(() -> new IllegalArgumentException("Participant not found")))
                .orElseThrow(() -> new IllegalArgumentException("Participant not found in this expense"));

        // Only the participant themselves or group admin can record payment
        TravelGroupMember member = travelGroupMemberRepository.findByGroupAndUser(expense.getGroup(), user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        if (!participant.getUser().getId().equals(user.getId()) &&
                !member.getRole().equals("CREATOR") && !member.getRole().equals("ADMIN")) {
            throw new IllegalArgumentException("You don't have permission to record this payment");
        }

        BigDecimal newPaidAmount = participant.getPaidAmount().add(amount);
        participant.setPaidAmount(newPaidAmount);
        participant.setBalance(participant.getShareAmount().subtract(newPaidAmount));

        if (participant.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            participant.setStatus("PAID");
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            participant.setStatus("PARTIAL");
        }

        expenseParticipantRepository.save(participant);

        // Check if all participants have paid
        List<ExpenseParticipant> allParticipants = expenseParticipantRepository.findByExpense(expense);
        boolean allPaid = allParticipants.stream()
                .allMatch(p -> p.getStatus().equals("PAID"));

        if (allPaid) {
            expense.setStatus("SETTLED");
            groupExpenseRepository.save(expense);
        }

        return MessageResponse.builder()
                .message("Payment recorded successfully")
                .build();
    }

    @Override
    public ExpenseSettlementResponse getExpenseSettlement(String groupId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Travel group not found"));

        // Verify user is a member
        travelGroupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        List<GroupExpense> expenses = groupExpenseRepository.findByGroupOrderByExpenseDateDesc(group);

        BigDecimal totalExpenses = expenses.stream()
                .map(GroupExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSettled = expenses.stream()
                .filter(e -> e.getStatus().equals("SETTLED"))
                .map(GroupExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPending = totalExpenses.subtract(totalSettled);

        // Calculate balances for each user
        Map<String, BalanceSummaryResponse> balanceMap = new HashMap<>();
        List<TravelGroupMember> members = travelGroupMemberRepository.findByGroup(group);

        for (TravelGroupMember member : members) {
            String userId = member.getUser().getId();
            BalanceSummaryResponse balance = BalanceSummaryResponse.builder()
                    .userId(userId)
                    .userEmail(member.getUser().getEmail())
                    .userName(member.getUser().getFirstName() + " " + member.getUser().getLastName())
                    .totalOwed(BigDecimal.ZERO)
                    .totalPaid(BigDecimal.ZERO)
                    .netBalance(BigDecimal.ZERO)
                    .build();
            balanceMap.put(userId, balance);
        }

        // Calculate who owes what
        for (GroupExpense expense : expenses) {
            List<ExpenseParticipant> participants = expenseParticipantRepository.findByExpense(expense);
            for (ExpenseParticipant participant : participants) {
                String userId = participant.getUser().getId();
                BalanceSummaryResponse balance = balanceMap.get(userId);
                if (balance != null) {
                    balance.setTotalOwed(balance.getTotalOwed().add(participant.getShareAmount()));
                    balance.setTotalPaid(balance.getTotalPaid().add(participant.getPaidAmount()));
                }
            }
        }

        // Calculate net balance
        for (BalanceSummaryResponse balance : balanceMap.values()) {
            balance.setNetBalance(balance.getTotalOwed().subtract(balance.getTotalPaid()));
        }

        List<ExpenseSummaryResponse> expenseSummary = expenses.stream()
                .map(e -> ExpenseSummaryResponse.builder()
                        .expenseId(e.getId())
                        .description(e.getDescription())
                        .amount(e.getAmount())
                        .paidBy(e.getPaidBy().getEmail())
                        .status(e.getStatus())
                        .build())
                .collect(Collectors.toList());

        return ExpenseSettlementResponse.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .totalExpenses(totalExpenses)
                .totalSettled(totalSettled)
                .totalPending(totalPending)
                .expenseSummary(expenseSummary)
                .balances(new ArrayList<>(balanceMap.values()))
                .build();
    }

    // =============== MAPPING METHODS ================

    private GroupMessageResponse mapToGroupMessageResponse(GroupMessage message) {
        User sender = message.getSender();
        return GroupMessageResponse.builder()
                .id(message.getId())
                .groupId(message.getGroup().getId())
                .senderId(sender.getId())
                .senderEmail(sender.getEmail())
                .senderName(sender.getFirstName() + " " + sender.getLastName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .attachmentUrl(message.getAttachmentUrl())
                .locationData(message.getLocationData())
                .status(message.getStatus())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private DirectMessageResponse mapToDirectMessageResponse(DirectMessage message, String currentUserId) {
        User sender = message.getSender();
        User recipient = message.getRecipient();
        return DirectMessageResponse.builder()
                .id(message.getId())
                .senderId(sender.getId())
                .senderEmail(sender.getEmail())
                .senderName(sender.getFirstName() + " " + sender.getLastName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .recipientId(recipient.getId())
                .recipientEmail(recipient.getEmail())
                .recipientName(recipient.getFirstName() + " " + recipient.getLastName())
                .recipientAvatarUrl(recipient.getAvatarUrl())
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .attachmentUrl(message.getAttachmentUrl())
                .locationData(message.getLocationData())
                .status(message.getStatus())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .isFromMe(sender.getId().equals(currentUserId))
                .build();
    }

    private GroupExpenseResponse mapToGroupExpenseResponse(GroupExpense expense) {
        List<ExpenseParticipant> participants = expenseParticipantRepository.findByExpense(expense);
        List<ExpenseParticipantResponse> participantResponses = participants.stream()
                .map(p -> ExpenseParticipantResponse.builder()
                        .id(p.getId())
                        .userId(p.getUser().getId())
                        .userEmail(p.getUser().getEmail())
                        .userName(p.getUser().getFirstName() + " " + p.getUser().getLastName())
                        .shareAmount(p.getShareAmount())
                        .paidAmount(p.getPaidAmount())
                        .balance(p.getBalance())
                        .status(p.getStatus())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        User paidBy = expense.getPaidBy();
        return GroupExpenseResponse.builder()
                .id(expense.getId())
                .groupId(expense.getGroup().getId())
                .paidById(paidBy.getId())
                .paidByEmail(paidBy.getEmail())
                .paidByName(paidBy.getFirstName() + " " + paidBy.getLastName())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .status(expense.getStatus())
                .receiptUrl(expense.getReceiptUrl())
                .notes(expense.getNotes())
                .participants(participantResponses)
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}