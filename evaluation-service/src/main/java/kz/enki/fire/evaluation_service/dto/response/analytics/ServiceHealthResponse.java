package kz.enki.fire.evaluation_service.dto.response.analytics;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceHealthResponse {
    String status;
    String timestamp;
    long ticketsTotal;
    long assignedTotal;
    long unassignedTotal;
    long highPriorityUnassigned;
}
