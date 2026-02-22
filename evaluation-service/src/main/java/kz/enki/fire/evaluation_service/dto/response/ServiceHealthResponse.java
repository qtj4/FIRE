package kz.enki.fire.evaluation_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthResponse {
    private String status;
    private String timestamp;
    private long ticketsTotal;
    private long assignedTotal;
    private long unassignedTotal;
    private long highPriorityUnassigned;
}
