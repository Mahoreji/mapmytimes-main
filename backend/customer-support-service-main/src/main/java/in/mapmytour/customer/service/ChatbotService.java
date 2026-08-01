package in.mapmytour.customer.service;

import in.mapmytour.customer.dto.ChatbotRequest;
import in.mapmytour.customer.dto.ChatbotResponse;

/**
 * Service for AI chatbot functionality
 */
public interface ChatbotService {
    
    /**
     * Process chatbot message and generate response
     */
    ChatbotResponse processMessage(ChatbotRequest request);
    
    /**
     * Get suggested responses/articles based on context
     */
    ChatbotResponse.Suggestions getSuggestions(String context);
    
    /**
     * Detect intent from user message
     */
    String detectIntent(String message);
    
    /**
     * Check if message requires human agent escalation
     */
    boolean requiresHumanAgent(String message, String intent);
}

