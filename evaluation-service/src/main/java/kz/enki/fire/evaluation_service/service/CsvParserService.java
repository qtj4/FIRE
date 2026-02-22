package kz.enki.fire.evaluation_service.service;

import kz.enki.fire.evaluation_service.dto.response.IntakeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

@Service
@Slf4j
public class CsvParserService {

    public <T> IntakeResponse parseAndProcess(MultipartFile file, Class<T> clazz, Consumer<List<T>> processor) {
        if (file.isEmpty()) {
            return IntakeResponse.builder()
                    .status("ERROR")
                    .message("File is empty")
                    .build();
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String normalizedCsv = readNormalizedCsv(reader);
            if (normalizedCsv.isBlank()) {
                return IntakeResponse.builder()
                        .status("ERROR")
                        .message("File is empty")
                        .build();
            }

            char separator = detectSeparator(normalizedCsv);
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(new StringReader(normalizedCsv))
                    .withType(clazz)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withSeparator(separator)
                    .withThrowExceptions(false)
                    .build();

            List<T> items = csvToBean.parse();
            int failedCount = csvToBean.getCapturedExceptions().size();

            if (!items.isEmpty()) {
                processor.accept(items);
            }

            csvToBean.getCapturedExceptions().forEach(e ->
                    log.warn("Error parsing CSV line: {}", e.getMessage())
            );

            return IntakeResponse.builder()
                    .status("SUCCESS")
                    .message("Processing completed")
                    .processedCount(items.size())
                    .failedCount(failedCount)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse CSV file", e);
            return IntakeResponse.builder()
                    .status("ERROR")
                    .message("Failed to parse CSV: " + e.getMessage())
                    .build();
        }
    }

    private static String readNormalizedCsv(Reader reader) throws Exception {
        BufferedReader bufferedReader = reader instanceof BufferedReader
                ? (BufferedReader) reader
                : new BufferedReader(reader);
        StringBuilder content = new StringBuilder();
        String line;
        boolean first = true;
        while ((line = bufferedReader.readLine()) != null) {
            if (first) {
                first = false;
                line = stripBom(line);
            }
            content.append(line).append('\n');
        }
        return content.toString();
    }

    private static char detectSeparator(String csv) {
        String header = csv.lines().findFirst().orElse("");
        long semicolons = header.chars().filter(ch -> ch == ';').count();
        long commas = header.chars().filter(ch -> ch == ',').count();
        return semicolons > commas ? ';' : ',';
    }

    private static String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }
}
