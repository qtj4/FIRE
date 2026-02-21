package kz.enki.fire.evaluation_service.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Простой формат сообщения в очереди incoming_tickets.
 * Пример: { "type": "...", "sentiment": "...", "priority": 8, "language": "RU", "summary": "...", "geo_normalized": null, "clientGuid": "uuid" }
 */
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
