package in.mapmytour.customer.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationRequest {
    private String message;
    private String attachmentUrl;
    private Boolean isInternalNote;
}