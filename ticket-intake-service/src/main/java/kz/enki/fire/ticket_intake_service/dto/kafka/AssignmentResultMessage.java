package kz.enki.fire.ticket_intake_service.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Результат назначения из evaluation-service (топик final_distribution). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResultMessage {
    private UUID clientGuid;
    private Long rawTicketId;
    private Long enrichedTicketId;
    private Long assignedManagerId;
    private String assignedManagerName;
    private Long assignedOfficeId;
    private String assignedOfficeName;
    private String status;
}
