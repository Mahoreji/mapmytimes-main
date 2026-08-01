package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponse {
    private String feedbackId;
    private String message;
    private String category;
    private Integer rating;
    private String subject;
    private LocalDateTime submittedAt;
    private String status;
    private String adminResponse;
    private LocalDateTime respondedAt;
}