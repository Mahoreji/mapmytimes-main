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
public class DocumentResponse {
    private String documentId;
    private String documentType;
    private String fileName;
    private Long fileSize;
    private LocalDateTime uploadDate;
    private String status;
    private String downloadUrl;
}