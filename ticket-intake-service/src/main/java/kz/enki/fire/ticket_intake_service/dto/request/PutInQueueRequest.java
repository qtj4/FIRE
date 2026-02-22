package kz.enki.fire.ticket_intake_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Тело для теста через Swagger: положить тикет в очередь.
 * evaluation-service подхватит из Kafka и обработает (создаст тикет, назначит менеджера).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные тикета для отправки в очередь (формат как из N8n)")
public class PutInQueueRequest {

    @Schema(description = "Тип обращения", example = "Неработоспособность приложения")
    private String type;

    @Schema(description = "Тональность", example = "Негативный")
    private String sentiment;

    @Schema(description = "Приоритет (1-10)", example = "8")
    private Integer priority;

    @Schema(description = "Язык", example = "RU")
    private String language;

    @Schema(description = "Краткое описание и рекомендации менеджеру")
    private String summary;

    @JsonProperty("geo_normalized")
    @Schema(description = "Нормализованный адрес для геокодинга (опционально)")
    private String geoNormalized;

    @Schema(description = "Опционально: clientGuid для идемпотентности. Если не передан — генерируется новый.")
    private UUID clientGuid;
}
