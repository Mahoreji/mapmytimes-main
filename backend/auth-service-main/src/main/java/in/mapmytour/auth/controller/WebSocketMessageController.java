package in.mapmytour.auth.controller;

import in.mapmytour.auth.dto.user.*;
import in.mapmytour.auth.service.PresenceService;
import in.mapmytour.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageController {

    private final UserService userService;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle group messages sent via WebSocket
     * Client sends to: /app/group/{groupId}/message
     * Server broadcasts to: /topic/group/{groupId}
     */
    @MessageMapping("/group/{groupId}/message")
    public void sendGroupMessage(@Payload GroupMessageRequest request, 
                                Principal principal,
                                @org.springframework.messaging.handler.annotation.DestinationVariable String groupId) {
        try {
            String userEmail = principal.getName();
            log.info("Received group message from {} for group {}", userEmail, groupId);
            
            var response = userService.sendGroupMessage(groupId, request, userEmail);
            
            // Broadcast to all subscribers of this group
            messagingTemplate.convertAndSend("/topic/group/" + groupId, response);
        } catch (Exception e) {
            log.error("Error handling group message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle direct messages sent via WebSocket
     * Client sends to: /app/direct/message
     * Server sends to: /queue/messages/{recipientEmail}
     */
    @MessageMapping("/direct/message")
    public void sendDirectMessage(@Payload DirectMessageRequest request, 
                                 Principal principal) {
        try {
            String senderEmail = principal.getName();
            log.info("Received direct message from {} to {}", senderEmail, request.getRecipientEmail());
            
            var response = userService.sendDirectMessage(request, senderEmail);
            
            // Send to recipient's personal queue
            messagingTemplate.convertAndSendToUser(
                request.getRecipientEmail(), 
                "/queue/messages", 
                response
            );
            
            // Also send confirmation back to sender
            messagingTemplate.convertAndSendToUser(
                senderEmail,
                "/queue/messages",
                response
            );
        } catch (Exception e) {
            log.error("Error handling direct message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle typing indicators for groups
     * Client sends to: /app/group/{groupId}/typing
     * Server broadcasts to: /topic/group/{groupId}/typing
     */
    @MessageMapping("/group/{groupId}/typing")
    public void handleGroupTyping(@Payload TypingIndicator indicator,
                            Principal principal,
                            @org.springframework.messaging.handler.annotation.DestinationVariable String groupId) {
        try {
            String userEmail = principal.getName();
            indicator.setUserEmail(userEmail);
            presenceService.updateLastSeen(userEmail);
            messagingTemplate.convertAndSend("/topic/group/" + groupId + "/typing", indicator);
        } catch (Exception e) {
            log.error("Error handling group typing indicator: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle typing indicators for direct messages
     * Client sends to: /app/direct/typing
     * Server sends to: /user/queue/typing
     */
    @MessageMapping("/direct/typing")
    public void handleDirectTyping(@Payload DirectTypingRequest request,
                                  Principal principal) {
        try {
            String senderEmail = principal.getName();
            presenceService.updateLastSeen(senderEmail);
            
            // Get sender user info
            var sender = userService.getCurrentUser(senderEmail);
            
            TypingIndicatorResponse typingIndicator = TypingIndicatorResponse.builder()
                    .userId(sender.getId())
                    .email(sender.getEmail())
                    .firstName(sender.getFirstName())
                    .lastName(sender.getLastName())
                    .avatarUrl(sender.getAvatarUrl())
                    .isTyping(request.isTyping())
                    .conversationId(request.getRecipientEmail())
                    .build();
            
            // Send to recipient
            messagingTemplate.convertAndSendToUser(
                    request.getRecipientEmail(),
                    "/queue/typing",
                    typingIndicator
            );
            
            log.debug("Typing indicator sent from {} to {}", senderEmail, request.getRecipientEmail());
        } catch (Exception e) {
            log.error("Error handling direct typing indicator: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle heartbeat/ping to keep connection alive and update presence
     * Client sends to: /app/heartbeat
     */
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(Principal principal) {
        try {
            String userEmail = principal.getName();
            presenceService.updateLastSeen(userEmail);
            // Optionally send acknowledgment back
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/heartbeat",
                    "OK"
            );
        } catch (Exception e) {
            log.error("Error handling heartbeat: {}", e.getMessage(), e);
        }
    }

    /**
     * Simple DTO for typing indicators
     */
    public static class TypingIndicator {
        private String userEmail;
        private boolean isTyping;

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public boolean isTyping() {
            return isTyping;
        }

        public void setTyping(boolean typing) {
            isTyping = typing;
        }
    }

    /**
     * Request DTO for direct message typing indicators
     */
    public static class DirectTypingRequest {
        private String recipientEmail;
        private boolean isTyping;

        public String getRecipientEmail() {
            return recipientEmail;
        }

        public void setRecipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
        }

        public boolean isTyping() {
            return isTyping;
        }

        public void setTyping(boolean typing) {
            isTyping = typing;
        }
    }
}

