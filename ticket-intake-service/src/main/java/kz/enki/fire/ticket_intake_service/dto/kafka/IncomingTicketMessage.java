package kz.enki.fire.ticket_intake_service.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** Формат сообщения в incoming_tickets — evaluation-service создаёт тикет и назначает менеджера. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingTicketMessage {
    private UUID clientGuid;
    private Long rawTicketId;
    private String type;
    private String sentiment;
    private Integer priority;
    private String language;
    private String summary;
    @JsonProperty("geo_normalized")
    private String geoNormalized;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
