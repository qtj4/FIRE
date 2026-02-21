package kz.enki.fire.ticket_intake_service.dto.response;

import lombok.Data;

@Data
public class EvaluationAssignmentResponse {
    private Long enrichedTicketId;
    private Long assignedManagerId;
    private String assignedManagerName;
    private Long assignedOfficeId;
    private String assignedOfficeName;
    private String status;
    private String message;
}
