package in.mapmytour.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    private String query;
    private String category;
    private String status;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;
}
