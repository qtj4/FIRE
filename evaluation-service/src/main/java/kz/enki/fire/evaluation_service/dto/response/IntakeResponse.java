package kz.enki.fire.evaluation_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntakeResponse {
    private String status;
    private String message;
    private int processedCount;
    private int failedCount;
}
