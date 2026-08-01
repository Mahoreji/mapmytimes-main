package in.mapmytour.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String language; // Language code (en, es, fr, etc.)
    private String context; // Conversation context
    private List<String> conversationHistory; // Previous messages for context
    private String bookingId; // If related to a booking
}

