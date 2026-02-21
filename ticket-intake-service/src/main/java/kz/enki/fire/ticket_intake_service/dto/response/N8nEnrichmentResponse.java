package kz.enki.fire.ticket_intake_service.dto.response;

import lombok.Data;

@Data
public class N8nEnrichmentResponse {
    private String type;
    private String sentiment;
    private Integer priority;
    private String language;
    private String summary;
    private String geo_normalized;
}
