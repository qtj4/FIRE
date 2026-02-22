package kz.enki.fire.ticket_intake_service.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class EvaluationAssignRequest {
    private Long rawTicketId;
    private UUID clientGuid;
    private String type;
    private Integer priority;
    private String summary;
    private String language;
    private String sentiment;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
