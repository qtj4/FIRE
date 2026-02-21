package kz.enki.fire.ticket_intake_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TicketIntakeResponse {
    private String status;
    private String message;
    private int processedCount;
    private int failedCount;
    private List<TicketProcessingResult> results;
}
