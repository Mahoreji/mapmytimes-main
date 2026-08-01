
package in.mapmytour.customer.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private String id;
    private String ticketId;
    private String customerId;
    private Integer rating;
    private String comments;
    private boolean isFollowUpRequired;
    private LocalDateTime submittedAt;
}