package kz.enki.fire.ticket_intake_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IntakeResponse {
    private String status;
    private String message;
    private int processedCount;
    private int failedCount;
    /** По каждому тикету: IN_QUEUE сразу после отправки, потом фронт опрашивает и получает ASSIGNED/FAILED. */
    private List<TicketProcessingResultDto> results;
}
