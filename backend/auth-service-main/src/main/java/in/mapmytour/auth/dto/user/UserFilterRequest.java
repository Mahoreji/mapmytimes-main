package in.mapmytour.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterRequest {
    private String search;
    private String role;
    private Boolean isActive;
    private Boolean isVerified;
    private String dateFrom;
    private String dateTo;
    private String sortBy;
    private String sortDirection;
}