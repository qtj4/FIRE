package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.evaluation_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.evaluation_service.dto.response.IntakeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvParserServiceTest {

    private CsvParserService csvParserService;

    @Mock
    private Consumer<List<OfficeCsvRequest>> officeProcessor;

    @Mock
    private Consumer<List<ManagerCsvRequest>> managerProcessor;

    @BeforeEach
    void setUp() {
        csvParserService = new CsvParserService();
    }

    @Nested
    @DisplayName("parseAndProcess - офисы")
    class OfficeParsing {

        @Test
        @DisplayName("возвращает ERROR при пустом файле")
        void emptyFile_returnsError() {
            MockMultipartFile file = new MockMultipartFile("file", "offices.csv", "text/csv", new byte[0]);

            IntakeResponse response = csvParserService.parseAndProcess(file, OfficeCsvRequest.class, officeProcessor);

            assertThat(response.getStatus()).isEqualTo("ERROR");
            assertThat(response.getMessage()).isEqualTo("File is empty");
            verifyNoInteractions(officeProcessor);
        }

        @Test
        @DisplayName("парсит валидный CSV офисов и вызывает processor")
        void validOfficeCsv_callsProcessor() {
            String csv = "Офис,Адрес,Широта,Долгота\nАстана,ул. Тест 1,51.1694,71.4491\nАлматы,ул. Тест 2,43.2389,76.9457";
            MockMultipartFile file = new MockMultipartFile("file", "offices.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

            IntakeResponse response = csvParserService.parseAndProcess(file, OfficeCsvRequest.class, officeProcessor);

            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(response.getProcessedCount()).isEqualTo(2);

            ArgumentCaptor<List<OfficeCsvRequest>> captor = ArgumentCaptor.forClass(List.class);
            verify(officeProcessor).accept(captor.capture());
            List<OfficeCsvRequest> requests = captor.getValue();
            assertThat(requests).hasSize(2);
            assertThat(requests.get(0).getName()).isEqualTo("Астана");
            assertThat(requests.get(0).getLatitude()).isEqualByComparingTo("51.1694");
            assertThat(requests.get(1).getName()).isEqualTo("Алматы");
        }

        @Test
        @DisplayName("не вызывает processor при пустом результате парсинга")
        void emptyParseResult_doesNotCallProcessor() {
            String csv = "Офис,Адрес,Широта,Долгота\n";
            MockMultipartFile file = new MockMultipartFile("file", "offices.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

            IntakeResponse response = csvParserService.parseAndProcess(file, OfficeCsvRequest.class, officeProcessor);

            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(response.getProcessedCount()).isEqualTo(0);
            verifyNoInteractions(officeProcessor);
        }
    }

    @Nested
    @DisplayName("parseAndProcess - менеджеры")
    class ManagerParsing {

        @Test
        @DisplayName("парсит валидный CSV менеджеров")
        void validManagerCsv_callsProcessor() {
            String csv = "ФИО,Должность,Офис,Навыки,Количество обращений в работе\nИванов И.И.,менеджер,Астана,RU VIP,5";
            MockMultipartFile file = new MockMultipartFile("file", "managers.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

            IntakeResponse response = csvParserService.parseAndProcess(file, ManagerCsvRequest.class, managerProcessor);

            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(response.getProcessedCount()).isEqualTo(1);

            ArgumentCaptor<List<ManagerCsvRequest>> captor = ArgumentCaptor.forClass(List.class);
            verify(managerProcessor).accept(captor.capture());
            List<ManagerCsvRequest> requests = captor.getValue();
            assertThat(requests).hasSize(1);
            assertThat(requests.get(0).getFullName()).isEqualTo("Иванов И.И.");
            assertThat(requests.get(0).getOfficeName()).isEqualTo("Астана");
            assertThat(requests.get(0).getSkills()).isEqualTo("RU VIP");
            assertThat(requests.get(0).getActiveTicketsCount()).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("возвращает ERROR при ошибке парсинга")
    void parseException_returnsError() {
        MockMultipartFile file = new MockMultipartFile("file", "data.bin", "application/octet-stream", new byte[]{0x00, 0x01, 0x02});

        IntakeResponse response = csvParserService.parseAndProcess(file, OfficeCsvRequest.class, officeProcessor);

        assertThat(response.getStatus()).isEqualTo("ERROR");
        assertThat(response.getMessage()).startsWith("Failed to parse CSV:");
        verifyNoInteractions(officeProcessor);
    }
}
