package in.mapmytour.auth.dto.user;

import in.mapmytour.auth.entity.PlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request for creating a TODAY_PLAN post in a circle.
 */
@Data
public class TodayPlanRequest {

    @NotNull
    private PlanType planType;

    private String message;
}
