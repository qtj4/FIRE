package kz.enki.fire.evaluation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisResult {
    private String sentiment;
    private Integer priority;
    private String language;
    private String summary;
    private String geoNormalized;
}
