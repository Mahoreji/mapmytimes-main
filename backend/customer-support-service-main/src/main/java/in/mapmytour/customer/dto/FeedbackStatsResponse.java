package in.mapmytour.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStatsResponse {

    private double averageRating;
    private int totalFeedbacks;
    private int positiveFeedbacks; // 4-5 stars
    private int neutralFeedbacks;  // 3 stars
    private int negativeFeedbacks; // 1-2 stars
}
