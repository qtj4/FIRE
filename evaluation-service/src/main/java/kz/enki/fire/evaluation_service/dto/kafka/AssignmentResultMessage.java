package kz.enki.fire.evaluation_service.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Результат назначения — уходит в ticket service по топику final_distribution.
 */
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
