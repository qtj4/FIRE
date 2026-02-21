package kz.enki.fire.ticket_intake_service.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import kz.enki.fire.ticket_intake_service.dto.response.IntakeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Service
@Slf4j
public class CsvParserService {

    public <T> CsvParseResult<T> parse(MultipartFile file, Class<T> clazz) {
        if (file.isEmpty()) {
            return CsvParseResult.<T>builder()
                    .status("ERROR")
                    .message("File is empty")
                    .items(Collections.emptyList())
                    .failedCount(0)
                    .build();
        }

        try (InputStream inputStream = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            reader.mark(1);
            if (reader.read() != 0xFEFF) {
                reader.reset();
            }

            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withType(clazz)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withThrowExceptions(false)
                    .build();

            List<T> items = csvToBean.parse();
            int failedCount = csvToBean.getCapturedExceptions().size();
            csvToBean.getCapturedExceptions().forEach(e ->
                    log.warn("Error parsing CSV line: {}", e.getMessage())
            );

            return CsvParseResult.<T>builder()
                    .status("SUCCESS")
                    .message("Parsing completed")
                    .items(items)
                    .failedCount(failedCount)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse CSV file", e);
            return CsvParseResult.<T>builder()
                    .status("ERROR")
                    .message("Failed to parse CSV: " + e.getMessage())
                    .items(Collections.emptyList())
                    .failedCount(0)
                    .build();
        }
    }

    public <T> IntakeResponse parseAndProcess(MultipartFile file, Class<T> clazz, Consumer<List<T>> processor) {
        CsvParseResult<T> parseResult = parse(file, clazz);
        if ("ERROR".equalsIgnoreCase(parseResult.getStatus())) {
            return IntakeResponse.builder()
                    .status(parseResult.getStatus())
                    .message(parseResult.getMessage())
                    .build();
        }

        if (!parseResult.getItems().isEmpty()) {
            processor.accept(parseResult.getItems());
        }

        return IntakeResponse.builder()
                .status("SUCCESS")
                .message("Processing completed")
                .processedCount(parseResult.getItems().size())
                .failedCount(parseResult.getFailedCount())
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    public static class CsvParseResult<T> {
        private String status;
        private String message;
        private List<T> items;
        private int failedCount;
    }
}
