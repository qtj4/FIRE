package kz.enki.fire.ticket_intake_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Один результат по тикету для ответа импорта / опроса. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketProcessingResultDto {
    private UUID clientGuid;
    private Long rawTicketId;
    private Long enrichedTicketId;
    private String status;           // IN_QUEUE, ASSIGNED, UNASSIGNED, FAILED
    private String assignedOfficeName;
    private String assignedManagerName;
    private Integer priority;
    private String message;
}
