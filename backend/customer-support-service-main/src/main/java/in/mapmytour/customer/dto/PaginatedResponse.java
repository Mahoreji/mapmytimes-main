package in.mapmytour.customer.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalItems;
    private int itemsPerPage;
}