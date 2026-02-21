package kz.enki.fire.evaluation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalDistribution {
    private Long managerId;
    private String managerName;
    private String sentiment;
    private Integer priority;
    private String language;
    private String summary;
    private LocalDateTime timestamp;
}
