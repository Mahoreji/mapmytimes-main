package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.dto.ChatbotRequest;
import in.mapmytour.customer.dto.ChatbotResponse;
import in.mapmytour.customer.service.ChatbotService;
import in.mapmytour.customer.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public ChatbotResponse processMessage(ChatbotRequest request) {
        log.debug("Processing chatbot message: {}", request.getMessage());
        
        String message = request.getMessage().toLowerCase();
        String intent = detectIntent(message);
        double confidence = calculateConfidence(message, intent);
        boolean requiresHuman = requiresHumanAgent(message, intent);
        
        String response = generateResponse(message, intent, requiresHuman);
        
        // Get related knowledge base articles
        List<ChatbotResponse.KnowledgeBaseArticle> relatedArticles = new ArrayList<>();
        if (!requiresHuman) {
            try {
                var articles = knowledgeBaseService.searchArticles(message, 
                        org.springframework.data.domain.PageRequest.of(0, 3));
                relatedArticles = articles.getContent().stream()
                        .map(article -> ChatbotResponse.KnowledgeBaseArticle.builder()
                                .id(article.getId())
                                .title(article.getTitle())
                                .summary(article.getTitle()) // Use title as summary if summary not available
                                .build())
                        .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                log.debug("Could not fetch related articles: {}", e.getMessage());
            }
        }
        
        return ChatbotResponse.builder()
                .response(response)
                .intent(intent)
                .confidence(confidence)
                .requiresHumanAgent(requiresHuman)
                .suggestions(ChatbotResponse.Suggestions.builder()
                        .quickReplies(getQuickReplies(intent))
                        .relatedArticles(relatedArticles)
                        .build())
                .build();
    }

    @Override
    public ChatbotResponse.Suggestions getSuggestions(String context) {
        List<String> quickReplies = Arrays.asList(
                "How to book a tour?",
                "Payment issues",
                "Cancel my booking",
                "Change my booking",
                "Contact support"
        );
        
        return ChatbotResponse.Suggestions.builder()
                .quickReplies(quickReplies)
                .relatedArticles(new ArrayList<>())
                .build();
    }

    @Override
    public String detectIntent(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("book") || lowerMessage.contains("reserve") || 
            lowerMessage.contains("booking")) {
            return "BOOKING";
        }
        if (lowerMessage.contains("payment") || lowerMessage.contains("pay") || 
            lowerMessage.contains("refund")) {
            return "PAYMENT";
        }
        if (lowerMessage.contains("cancel") || lowerMessage.contains("cancellation")) {
            return "CANCELLATION";
        }
        if (lowerMessage.contains("change") || lowerMessage.contains("modify") || 
            lowerMessage.contains("update")) {
            return "MODIFICATION";
        }
        if (lowerMessage.contains("help") || lowerMessage.contains("support") || 
            lowerMessage.contains("issue")) {
            return "SUPPORT";
        }
        
        return "GENERAL";
    }

    @Override
    public boolean requiresHumanAgent(String message, String intent) {
        // Escalate to human if:
        // 1. User explicitly asks for human
        // 2. Complex payment/refund issues
        // 3. High confidence that intent is support/issue
        
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("human") || lowerMessage.contains("agent") || 
            lowerMessage.contains("representative")) {
            return true;
        }
        
        if (intent.equals("PAYMENT") && (lowerMessage.contains("refund") || 
            lowerMessage.contains("dispute"))) {
            return true;
        }
        
        return intent.equals("SUPPORT") && (lowerMessage.contains("urgent") || 
                lowerMessage.contains("critical") || lowerMessage.contains("emergency"));
    }

    private String generateResponse(String message, String intent, boolean requiresHuman) {
        if (requiresHuman) {
            return "I understand you need assistance. Let me connect you with a support agent who can help you better. " +
                   "A ticket has been created and an agent will respond shortly.";
        }
        
        switch (intent) {
            case "BOOKING":
                return "I can help you with booking! You can book a tour through our website or mobile app. " +
                       "Would you like me to guide you through the booking process?";
            case "PAYMENT":
                return "For payment-related questions, I can help you with common payment issues. " +
                       "If you're experiencing a specific problem, please provide more details.";
            case "CANCELLATION":
                return "I can help you with cancellation requests. Please note that cancellation policies " +
                       "vary by booking type. Would you like to know more about our cancellation policy?";
            case "MODIFICATION":
                return "I can help you modify your booking. Please provide your booking reference number " +
                       "and the changes you'd like to make.";
            case "SUPPORT":
                return "I'm here to help! Please describe your issue and I'll do my best to assist you. " +
                       "If I can't resolve it, I'll connect you with a support agent.";
            default:
                return "I'm here to help! How can I assist you today? You can ask me about bookings, payments, " +
                       "cancellations, or any other travel-related questions.";
        }
    }

    private double calculateConfidence(String message, String intent) {
        // Simple confidence calculation based on keyword matches
        int keywordMatches = 0;
        String[] keywords = switch (intent) {
            case "BOOKING" -> new String[]{"book", "reserve", "booking", "tour", "trip"};
            case "PAYMENT" -> new String[]{"payment", "pay", "refund", "money", "charge"};
            case "CANCELLATION" -> new String[]{"cancel", "cancellation", "refund"};
            case "MODIFICATION" -> new String[]{"change", "modify", "update", "edit"};
            case "SUPPORT" -> new String[]{"help", "support", "issue", "problem", "error"};
            default -> new String[]{};
        };
        
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                keywordMatches++;
            }
        }
        
        return Math.min(0.9, 0.5 + (keywordMatches * 0.1));
    }

    private List<String> getQuickReplies(String intent) {
        switch (intent) {
            case "BOOKING":
                return Arrays.asList("How to book?", "Payment methods", "Booking requirements");
            case "PAYMENT":
                return Arrays.asList("Payment failed", "Refund request", "Payment methods");
            case "CANCELLATION":
                return Arrays.asList("Cancellation policy", "Refund process", "Cancel booking");
            case "MODIFICATION":
                return Arrays.asList("Change dates", "Modify travelers", "Update booking");
            default:
                return Arrays.asList("Book a tour", "Payment help", "Contact support");
        }
    }
}

