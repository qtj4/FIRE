package kz.enki.fire.evaluation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.enki.fire.evaluation_service.dto.AIAnalysisResult;
import kz.enki.fire.evaluation_service.dto.FinalDistribution;
import kz.enki.fire.evaluation_service.dto.TicketContext;
import kz.enki.fire.evaluation_service.entity.Manager;
import kz.enki.fire.evaluation_service.service.EvaluationService;
import kz.enki.fire.evaluation_service.service.ManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluation")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Evaluation Service API", description = "API для интеллектуального распределения заявок")
public class EvaluationController {
    
    private final EvaluationService evaluationService;
    private final ManagerService managerService;

    @Operation(
        summary = "Обработать заявку",
        description = "Принимает заявку, выполняет AI-анализ и распределяет по менеджеру"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202",
            description = "Заявка принята в обработку",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Невалидные данные заявки"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка сервера"
        )
    })
    @PostMapping("/process-ticket")
    public ResponseEntity<String> processTicket(
            @Valid @RequestBody 
            @Parameter(description = "Данные заявки для обработки", required = true)
            TicketContext ticketContext) {
        
        log.info("Received ticket via REST API: location={}, language={}", 
                ticketContext.getLocation(), ticketContext.getLanguage());
        
        return evaluationService.processTicketSync(ticketContext);
    }

    @Operation(
        summary = "AI анализ текста",
        description = "Выполняет AI-анализ текста и возвращает результат"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Анализ выполнен успешно",
            content = @Content(schema = @Schema(implementation = AIAnalysisResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Пустой текст"
        )
    })
    @PostMapping("/analyze")
    public ResponseEntity<AIAnalysisResult> analyzeText(
            @Parameter(description = "Текст для анализа", required = true)
            @RequestParam String text) {
        
        return evaluationService.analyzeTextSync(text);
    }

    @Operation(
        summary = "Найти менеджеров по локации",
        description = "Возвращает список подходящих менеджеров для указанной локации"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список менеджеров получен",
            content = @Content(schema = @Schema(implementation = Manager.class))
        )
    })
    @GetMapping("/managers")
    public ResponseEntity<List<Manager>> findManagers(
            @Parameter(description = "Локация для поиска менеджеров", required = false)
            @RequestParam(defaultValue = "Almaty") String location) {
        
        return managerService.findManagersByLocationSync(location);
    }

    @Operation(
        summary = "Получить информацию о менеджере",
        description = "Возвращает детальную информацию о менеджере по ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Информация о менеджере получена",
            content = @Content(schema = @Schema(implementation = Manager.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Менеджер не найден"
        )
    })
    @GetMapping("/managers/{id}")
    public ResponseEntity<Manager> getManager(
            @Parameter(description = "ID менеджера", required = true)
            @PathVariable Long id) {
        
        return managerService.findManagerByIdSync(id);
    }

    @Operation(
        summary = "Получить нагрузку менеджера",
        description = "Возвращает текущую нагрузку менеджера из Redis"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Нагрузка получена",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Менеджер не найден"
        )
    })
    @GetMapping("/managers/{id}/load")
    public ResponseEntity<String> getManagerLoad(
            @Parameter(description = "ID менеджера", required = true)
            @PathVariable Long id) {
        
        return managerService.getManagerLoadSync(id);
    }

    @Operation(
        summary = "Симуляция распределения заявки",
        description = "Выполняет полный цикл обработки заявки и возвращает результат распределения"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Распределение выполнено",
            content = @Content(schema = @Schema(implementation = FinalDistribution.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Подходящий менеджер не найден"
        )
    })
    @PostMapping("/simulate-distribution")
    public ResponseEntity<FinalDistribution> simulateDistribution(
            @Valid @RequestBody 
            @Parameter(description = "Данные заявки для симуляции", required = true)
            TicketContext ticketContext) {
        
        return evaluationService.simulateDistributionSync(ticketContext);
    }

    @Operation(
        summary = "Проверка здоровья сервиса",
        description = "Возвращает статус здоровья сервиса"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Сервис работает нормально"
        )
    })
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Evaluation Service is healthy");
    }
}
