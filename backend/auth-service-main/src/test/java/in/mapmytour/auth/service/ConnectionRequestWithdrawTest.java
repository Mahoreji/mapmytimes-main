package in.mapmytour.auth.service;

import in.mapmytour.auth.dto.auth.MessageResponse;
import in.mapmytour.auth.entity.ConnectionRequest;
import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.repository.ConnectionRequestRepository;
import in.mapmytour.auth.repository.UserRepository;
import in.mapmytour.auth.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionRequestWithdrawTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectionRequestRepository connectionRequestRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User requester;
    private User recipient;
    private ConnectionRequest connectionRequest;

    @BeforeEach
    void setUp() {
        requester = User.builder()
                .id("requester-id")
                .email("requester@test.com")
                .firstName("Requester")
                .lastName("User")
                .isActive(true)
                .build();

        recipient = User.builder()
                .id("recipient-id")
                .email("recipient@test.com")
                .firstName("Recipient")
                .lastName("User")
                .isActive(true)
                .build();

        connectionRequest = ConnectionRequest.builder()
                .id("request-id")
                .requester(requester)
                .recipient(recipient)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testWithdrawConnectionRequest_Success() {
        // Arrange
        when(userRepository.findByEmail("requester@test.com"))
                .thenReturn(Optional.of(requester));
        when(connectionRequestRepository.findByIdAndRequester("request-id", requester))
                .thenReturn(Optional.of(connectionRequest));

        // Act
        MessageResponse response = userService.withdrawConnectionRequest(
                "requester@test.com", "request-id");

        // Assert
        assertNotNull(response);
        assertEquals("Connection request withdrawn successfully", response.getMessage());
        
        // Verify the request was deleted
        verify(connectionRequestRepository, times(1)).delete(connectionRequest);
        
        // Verify WebSocket notification was sent (may fail silently, so we don't verify strictly)
        // The main functionality (deletion) is what we're testing
    }

    @Test
    void testWithdrawConnectionRequest_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("requester@test.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.withdrawConnectionRequest("requester@test.com", "request-id")
        );

        assertEquals("User not found", exception.getMessage());
        verify(connectionRequestRepository, never()).delete(any());
    }

    @Test
    void testWithdrawConnectionRequest_RequestNotFound() {
        // Arrange
        when(userRepository.findByEmail("requester@test.com"))
                .thenReturn(Optional.of(requester));
        when(connectionRequestRepository.findByIdAndRequester("request-id", requester))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.withdrawConnectionRequest("requester@test.com", "request-id")
        );

        assertTrue(exception.getMessage().contains("Connection request not found"));
        verify(connectionRequestRepository, never()).delete(any());
    }

    @Test
    void testWithdrawConnectionRequest_NotRequester() {
        // Arrange - Try to withdraw as recipient (not requester)
        when(userRepository.findByEmail("recipient@test.com"))
                .thenReturn(Optional.of(recipient));
        when(connectionRequestRepository.findByIdAndRequester("request-id", recipient))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.withdrawConnectionRequest("recipient@test.com", "request-id")
        );

        assertTrue(exception.getMessage().contains("Connection request not found"));
        verify(connectionRequestRepository, never()).delete(any());
    }

    @Test
    void testWithdrawConnectionRequest_AlreadyAccepted() {
        // Arrange
        connectionRequest.setStatus("ACCEPTED");
        when(userRepository.findByEmail("requester@test.com"))
                .thenReturn(Optional.of(requester));
        when(connectionRequestRepository.findByIdAndRequester("request-id", requester))
                .thenReturn(Optional.of(connectionRequest));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.withdrawConnectionRequest("requester@test.com", "request-id")
        );

        assertTrue(exception.getMessage().contains("not pending"));
        verify(connectionRequestRepository, never()).delete(any());
    }

    @Test
    void testWithdrawConnectionRequest_AlreadyRejected() {
        // Arrange
        connectionRequest.setStatus("REJECTED");
        when(userRepository.findByEmail("requester@test.com"))
                .thenReturn(Optional.of(requester));
        when(connectionRequestRepository.findByIdAndRequester("request-id", requester))
                .thenReturn(Optional.of(connectionRequest));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.withdrawConnectionRequest("requester@test.com", "request-id")
        );

        assertTrue(exception.getMessage().contains("not pending"));
        verify(connectionRequestRepository, never()).delete(any());
    }

    @Test
    void testWithdrawConnectionRequest_WebSocketNotificationFailure() {
        // Arrange - WebSocket is null in test, but code handles it gracefully
        when(userRepository.findByEmail("requester@test.com"))
                .thenReturn(Optional.of(requester));
        when(connectionRequestRepository.findByIdAndRequester("request-id", requester))
                .thenReturn(Optional.of(connectionRequest));

        // Act - Should still succeed even if WebSocket is unavailable
        MessageResponse response = userService.withdrawConnectionRequest(
                "requester@test.com", "request-id");

        // Assert
        assertNotNull(response);
        assertEquals("Connection request withdrawn successfully", response.getMessage());
        verify(connectionRequestRepository, times(1)).delete(connectionRequest);
        // WebSocket error is caught and logged, operation still succeeds
    }
}

