package kz.enki.fire.evaluation_service.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedTicketEvent {
    private Long enrichedTicketId;
    private UUID clientGuid;
    private String type;
    private Integer priority;
    private String summary;
    private String language;
    private String sentiment;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime enrichedAt;
}
