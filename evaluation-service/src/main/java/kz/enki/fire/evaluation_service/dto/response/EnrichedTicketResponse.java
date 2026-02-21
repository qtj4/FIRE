package kz.enki.fire.evaluation_service.dto.response;

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
public class EnrichedTicketResponse {
    private Long id;
    private Long rawTicketId;
    private UUID clientGuid;
    private String type;
    private Integer priority;
    private String summary;
    private String language;
    private String sentiment;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long assignedOfficeId;
    private String assignedOfficeName;
    private Long assignedManagerId;
    private String assignedManagerName;
    private LocalDateTime enrichedAt;
}
