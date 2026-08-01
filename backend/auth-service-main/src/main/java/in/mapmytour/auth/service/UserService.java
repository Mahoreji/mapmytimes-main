package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.dto.auth.UserResponse;
import in.mapmytour.auth.dto.auth.UserStatsResponse;
import in.mapmytour.auth.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface UserService {

    // User Management
    UserProfileResponse getUserById(String userId);

    UserProfileResponse getUserById(String userId, String currentUserEmail);

    // Profile Management
    UserProfileResponse getCurrentUser(String userEmail);

    UserProfileResponse updateProfile(UpdateProfileRequest request, String userEmail);

    UserProfileResponse getPublicProfile(String userId);

    UserSummaryResponse getUserSummary(String userId);

    MessageResponse updateProfileVisibility(String userEmail, boolean isVisible);

    // Avatar Management
    AvatarUploadResponse uploadAvatar(MultipartFile file, String userEmail);

    MessageResponse deleteAvatar(String userEmail);

    // Cover Image Management
    AvatarUploadResponse uploadCoverImage(MultipartFile file, String userEmail);

    MessageResponse deleteCoverImage(String userEmail);

    String generateAvatarUrl(String userEmail, String initials);

    // Address Management
    UserProfileResponse updateAddress(AddressRequest request, String userEmail);

    MessageResponse deleteAddress(String userEmail);

    List<AddressResponse> getSavedAddresses(String userEmail);

    MessageResponse saveAddress(AddressRequest request, String userEmail, String label);

    // Preferences Management
    UserProfileResponse updatePreferences(UserPreferencesRequest request, String userEmail);

    UserPreferencesResponse getPreferences(String userEmail);

    MessageResponse resetPreferencesToDefault(String userEmail);

    // Notification Settings
    NotificationSettingsResponse getNotificationSettings(String userEmail);

    MessageResponse updateNotificationSettings(NotificationSettingsRequest request, String userEmail);

    MessageResponse subscribeToNotification(String userEmail, String notificationType);

    MessageResponse unsubscribeFromNotification(String userEmail, String notificationType);

    // Privacy Settings
    PrivacySettingsResponse getPrivacySettings(String userEmail);

    MessageResponse updatePrivacySettings(PrivacySettingsRequest request, String userEmail);

    MessageResponse blockUser(String userEmail, String targetUserId);

    MessageResponse unblockUser(String userEmail, String targetUserId);

    List<BlockedUserResponse> getBlockedUsers(String userEmail);

    // Account Management
    MessageResponse deleteAccount(String password, String userEmail);

    MessageResponse requestAccountVerification(String userEmail);

    AccountDataResponse exportAccountData(String userEmail);

    MessageResponse importAccountData(MultipartFile file, String userEmail);

    // Document Management
    DocumentUploadResponse uploadDocument(MultipartFile file, String documentType, String userEmail);

    List<DocumentResponse> getDocuments(String userEmail);

    MessageResponse deleteDocument(String userEmail, String documentId);

    DocumentResponse getDocument(String userEmail, String documentId);

    // Activity and Statistics
    UserActivityResponse getUserActivity(String userEmail, Pageable pageable);

    Page<AdminActivityItemResponse> getAllUserActivities(Pageable pageable, String userEmail); // Admin access

    MessageResponse trackActivity(TrackActivityRequest request, String userEmail, String ipAddress, String userAgent);

    UserStatsResponse getUserStatistics(String userEmail);

    LoginHistoryResponse getLoginHistory(String userEmail, Pageable pageable);

    // Social Features
    List<UserConnectionResponse> getConnections(String userEmail);

    List<UserConnectionResponse> getPublicConnections(String userId);

    List<UserConnectionResponse> getMutualConnections(String userEmail, String targetUserId);

    List<NotificationItemResponse> getIncomingConnectionRequests(String userEmail);

    void createSocialNotification(String recipientUserId, String senderUserId, String type, String message, String postId, String bookingId, String paymentId, String actionUrl);

    List<ConnectionRequestResponse> getOutgoingConnectionRequests(String userEmail);

    MessageResponse sendConnectionRequest(String userEmail, String targetUserId);

    MessageResponse acceptConnectionRequest(String userEmail, String requestId);

    MessageResponse rejectConnectionRequest(String userEmail, String requestId);

    MessageResponse withdrawConnectionRequest(String userEmail, String requestId);

    MessageResponse removeConnection(String userEmail, String connectionId);

    // Search and Discovery
    Page<UserProfileResponse> searchUsers(String query, Pageable pageable, String currentUserId);

    List<UserSuggestionResponse> getSuggestedUsers(String userEmail);

    List<UserProfileResponse> getNearbyUsers(String userEmail, double radius);

    List<UserProfileResponse> getNearbyTravelers(String userEmail, String destination, String filterByLocation);

    // Travel Plans
    TravelPlanResponse createTravelPlan(TravelPlanRequest request, String userEmail);

    List<TravelPlanResponse> getTravelPlans(String userEmail);

    TravelPlanResponse updateTravelPlan(String planId, TravelPlanRequest request, String userEmail);

    MessageResponse deleteTravelPlan(String planId, String userEmail);

    List<TravelPlanResponse> findTravelersByDestination(String destination);

    // Travel Groups
    TravelGroupResponse createTravelGroup(TravelGroupRequest request, String userEmail);

    TravelGroupResponse createTravelGroupWithImage(TravelGroupRequest request,
            org.springframework.web.multipart.MultipartFile file, String userEmail);

    TravelGroupResponse getTravelGroup(String groupId, String userEmail);

    List<TravelGroupResponse> getMyTravelGroups(String userEmail);

    TravelGroupResponse updateTravelGroup(String groupId, TravelGroupRequest request, String userEmail);

    TravelGroupResponse uploadGroupImage(String groupId, org.springframework.web.multipart.MultipartFile file,
            String userEmail);

    MessageResponse deleteTravelGroup(String groupId, String userEmail);

    MessageResponse joinTravelGroup(String inviteCode, String userEmail);

    MessageResponse leaveTravelGroup(String groupId, String userEmail);

    MessageResponse inviteToTravelGroup(String groupId, String userEmail, String inviteeEmail);

    List<TravelGroupResponse> discoverTravelGroups(String destination, String userEmail);

    // Travel Itinerary
    TravelItineraryResponse createItineraryItem(TravelItineraryRequest request, String userEmail);

    List<TravelItineraryResponse> getItinerary(String userEmail, LocalDate startDate, LocalDate endDate);

    List<TravelItineraryResponse> getItineraryByDate(String userEmail, LocalDate date);

    TravelItineraryResponse updateItineraryItem(String itemId, TravelItineraryRequest request, String userEmail);

    MessageResponse deleteItineraryItem(String itemId, String userEmail);

    List<TravelItineraryResponse> getGroupItinerary(String groupId, String userEmail);

    // Trip Matching
    List<TravelMatchResponse> findTravelMatches(String userEmail, String destination);

    List<TravelMatchResponse> getPersonalizedMatches(String userEmail);

    // User Verification
    MessageResponse submitVerificationRequest(VerificationRequest request, String userEmail);

    VerificationStatusResponse getVerificationStatus(String userEmail);

    MessageResponse uploadVerificationDocument(MultipartFile file, String documentType, String userEmail);

    // Admin Verification Management
    Page<VerificationRequestResponse> getAllVerificationRequests(org.springframework.data.domain.Pageable pageable,
            String status);

    VerificationRequestResponse getVerificationRequestById(String requestId);

    MessageResponse approveVerificationRequest(String requestId, String adminEmail, String adminNotes);

    MessageResponse rejectVerificationRequest(String requestId, String adminEmail, String adminNotes);

    // Subscription Management
    SubscriptionResponse getSubscription(String userEmail);

    MessageResponse upgradeSubscription(SubscriptionUpgradeRequest request, String userEmail);

    MessageResponse cancelSubscription(String userEmail);

    List<SubscriptionHistoryResponse> getSubscriptionHistory(String userEmail);

    // Communication Preferences
    CommunicationPreferencesResponse getCommunicationPreferences(String userEmail);

    MessageResponse updateCommunicationPreferences(CommunicationPreferencesRequest request, String userEmail);

    MessageResponse optOutFromEmails(String userEmail, String token);

    MessageResponse optInToEmails(String userEmail);

    // Emergency Contacts
    List<EmergencyContactResponse> getEmergencyContacts(String userEmail);

    MessageResponse addEmergencyContact(EmergencyContactRequest request, String userEmail);

    MessageResponse updateEmergencyContact(String contactId, EmergencyContactRequest request, String userEmail);

    MessageResponse removeEmergencyContact(String userEmail, String contactId);

    // Travel Preferences
    TravelPreferencesResponse getTravelPreferences(String userEmail);

    MessageResponse updateTravelPreferences(TravelPreferencesRequest request, String userEmail);

    // Interests and Tags
    List<InterestResponse> getInterests(String userEmail);

    MessageResponse updateInterests(List<String> interests, String userEmail);

    List<InterestResponse> getAvailableInterests();

    // User Feedback
    MessageResponse submitFeedback(FeedbackRequest request, String userEmail);

    List<FeedbackResponse> getUserFeedback(String userEmail);

    // Admin User Management
    Page<UserProfileResponse> getAllUsersForAdmin(Pageable pageable, UserFilterRequest filter);

    MessageResponse adminUpdateUserProfile(String userId, AdminUpdateUserRequest request);

    MessageResponse adminVerifyUser(String userId);

    MessageResponse adminSuspendUser(String userId, String reason);

    MessageResponse adminUnsuspendUser(String userId);

    UserDetailedResponse getDetailedUserInfo(String userId);

    // Bulk Operations
    MessageResponse bulkUpdateUsers(BulkUpdateRequest request);

    MessageResponse bulkDeleteUsers(List<String> userIds);

    MessageResponse bulkSendNotification(BulkNotificationRequest request);

    // Health Check
    MessageResponse healthCheck();

    // Group Messaging
    GroupMessageResponse sendGroupMessage(String groupId, GroupMessageRequest request, String userEmail);

    Page<GroupMessageResponse> getGroupMessages(String groupId, String userEmail, Pageable pageable);

    // Direct Messaging
    DirectMessageResponse sendDirectMessage(DirectMessageRequest request, String userEmail);

    Page<DirectMessageResponse> getConversation(String recipientEmail, String userEmail, Pageable pageable);

    List<UserProfileResponse> getConversationPartners(String userEmail);

    MessageResponse markMessageAsRead(String messageId, String userEmail);

    long getUnreadMessageCount(String userEmail);

    // Expense Management
    GroupExpenseResponse createGroupExpense(String groupId, GroupExpenseRequest request, String userEmail);

    List<GroupExpenseResponse> getGroupExpenses(String groupId, String userEmail);

    GroupExpenseResponse getExpenseDetails(String expenseId, String userEmail);

    MessageResponse updateExpense(String expenseId, GroupExpenseRequest request, String userEmail);

    MessageResponse deleteExpense(String expenseId, String userEmail);

    MessageResponse recordPayment(String expenseId, String participantId, BigDecimal amount, String userEmail);

    ExpenseSettlementResponse getExpenseSettlement(String groupId, String userEmail);
}