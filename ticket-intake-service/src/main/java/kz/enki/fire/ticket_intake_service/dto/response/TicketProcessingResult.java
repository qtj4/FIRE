package kz.enki.fire.ticket_intake_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketProcessingResult {
    private String clientGuid;
    private Long rawTicketId;
    private Long enrichedTicketId;
    private String status;
    private String message;
    private String assignedOfficeName;
    private String assignedManagerName;
    private Integer priority;
    private String language;
    private String type;
}
