package kz.enki.fire.evaluation_service.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class EnrichedTicketCreateRequest {
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
