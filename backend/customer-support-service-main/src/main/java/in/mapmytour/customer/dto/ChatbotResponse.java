package in.mapmytour.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    
    private String response;
    private String intent; // Detected intent (booking, payment, cancellation, etc.)
    private Double confidence; // Confidence score (0-1)
    private Boolean requiresHumanAgent; // Whether to escalate to human agent
    private Suggestions suggestions;
    private String ticketId; // If a ticket was created
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestions {
        private List<String> quickReplies;
        private List<KnowledgeBaseArticle> relatedArticles;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeBaseArticle {
        private String id;
        private String title;
        private String summary;
    }
}

