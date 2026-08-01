package in.mapmytour.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Request to create a poll in a trip circle.
 */
@Data
public class CirclePollRequest {

    @NotBlank
    private String question;

    @NotEmpty
    private List<String> options;

    private OffsetDateTime closesAt;
}
