package kz.enki.fire.evaluation_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketAssignmentResponse {
    private Long enrichedTicketId;
    private Long assignedManagerId;
    private String assignedManagerName;
    private Long assignedOfficeId;
    private String assignedOfficeCode;
    private String assignedOfficeName;
    private String status;
    private String message;
}
