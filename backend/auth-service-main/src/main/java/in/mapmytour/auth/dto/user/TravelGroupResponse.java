package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelGroupResponse {
    private String id;
    private String name;
    private String description;
    private String createdByUserId;
    private String createdByEmail;
    private String createdByName;
    private String destination;
    private LocalDate travelDate;
    private LocalDate returnDate;
    private Integer maxMembers;
    private Integer currentMembers;
    private String status;
    private String travelType;
    private Boolean isPublic;
    private String imageUrl;
    private String inviteCode;
    private List<GroupMemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

