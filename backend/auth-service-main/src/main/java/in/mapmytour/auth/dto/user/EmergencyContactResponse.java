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
public class EmergencyContactResponse {
    private String contactId;
    private String name;
    private String phone;
    private String email;
    private String relationship;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;
}