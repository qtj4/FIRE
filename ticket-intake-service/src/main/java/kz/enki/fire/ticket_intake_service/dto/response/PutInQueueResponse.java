package kz.enki.fire.ticket_intake_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Результат отправки тикета в очередь")
public class PutInQueueResponse {

    @Schema(description = "Client GUID (переданный или сгенерированный)")
    private UUID clientGuid;

    @Schema(description = "Сообщение для пользователя")
    private String message;
}
