package kz.enki.fire.evaluation_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class EnrichedTicketAssignRequest {
    private Long rawTicketId;
    private RawTicketRef rawTicket;
    private UUID clientGuid;
    private String type;
    private Integer priority;
    private String summary;
    private String language;
    private String sentiment;
    @JsonProperty("geo_normalized")
    private String geoNormalized;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Data
    public static class RawTicketRef {
        private Long id;
    }
}
