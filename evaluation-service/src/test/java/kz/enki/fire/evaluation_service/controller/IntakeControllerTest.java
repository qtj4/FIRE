package kz.enki.fire.evaluation_service.controller;

import kz.enki.fire.evaluation_service.dto.request.ManagerCsvRequest;
import kz.enki.fire.evaluation_service.dto.request.OfficeCsvRequest;
import kz.enki.fire.evaluation_service.dto.response.IntakeResponse;
import kz.enki.fire.evaluation_service.service.CsvParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IntakeController.class)
class IntakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CsvParserService csvParserService;

    @MockBean
    private kz.enki.fire.evaluation_service.service.OfficeService officeService;

    @MockBean
    private kz.enki.fire.evaluation_service.service.ManagerService managerService;

    @Test
    @DisplayName("POST /offices - принимает CSV и возвращает IntakeResponse")
    void postOffices_acceptsCsv_returnsResponse() throws Exception {
        String csv = "Офис,Адрес,Широта,Долгота\nАстана,ул. Тест,51.1694,71.4491";
        MockMultipartFile file = new MockMultipartFile("file", "offices.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(csvParserService.parseAndProcess(any(), eq(OfficeCsvRequest.class), any()))
                .thenReturn(IntakeResponse.builder()
                        .status("SUCCESS")
                        .message("Processing completed")
                        .processedCount(1)
                        .failedCount(0)
                        .build());

        mockMvc.perform(multipart("/api/v1/intake/offices").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.processedCount").value(1));
    }

    @Test
    @DisplayName("POST /managers - принимает CSV и возвращает IntakeResponse")
    void postManagers_acceptsCsv_returnsResponse() throws Exception {
        String csv = "ФИО,Должность,Офис,Навыки,Количество обращений в работе\nИванов,менеджер,Астана,RU,0";
        MockMultipartFile file = new MockMultipartFile("file", "managers.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(csvParserService.parseAndProcess(any(), eq(ManagerCsvRequest.class), any()))
                .thenReturn(IntakeResponse.builder()
                        .status("SUCCESS")
                        .message("Processing completed")
                        .processedCount(1)
                        .failedCount(0)
                        .build());

        mockMvc.perform(multipart("/api/v1/intake/managers").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.processedCount").value(1));
    }

    @Test
    @DisplayName("POST /offices - пустой файл возвращает ERROR")
    void postOffices_emptyFile_returnsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        when(csvParserService.parseAndProcess(any(), eq(OfficeCsvRequest.class), any()))
                .thenReturn(IntakeResponse.builder()
                        .status("ERROR")
                        .message("File is empty")
                        .build());

        mockMvc.perform(multipart("/api/v1/intake/offices").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("File is empty"));
    }
}
