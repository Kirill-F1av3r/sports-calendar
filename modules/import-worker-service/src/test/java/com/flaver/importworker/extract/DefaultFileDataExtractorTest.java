package com.flaver.importworker.extract;

import com.flaver.importworker.config.ImportProcessingProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFileDataExtractorTest {
    private final DefaultFileDataExtractor extractor = new DefaultFileDataExtractor(
            new ImportProcessingProperties(2, 2, 2, 10, 72));

    @Test
    void preservesImageBytesAndDescription() {
        byte[] bytes = {1, 2, 3};

        ExtractedFileData result = extractor.extract("photo.PNG", null, bytes);

        assertThat(result.kind()).isEqualTo(FileDataKind.IMAGE);
        assertThat(result.imageChunks()).hasSize(1);
        assertThat(result.imageChunks().getFirst().content()).containsExactly(bytes);
        assertThat(result.imageChunks().getFirst().sourceDescription()).contains("photo.PNG");
    }

    @Test
    void chunksPlainTextAndDropsBlankLines() {
        ExtractedFileData result = extractor.extract("events.txt", "text/plain",
                "one\n\n two \nthree".getBytes(StandardCharsets.UTF_8));

        assertThat(result.contentChunks()).hasSize(2);
        assertThat(result.contentChunks().getFirst()).contains("Lines 1-2", "Line 1: one", "Line 2: two");
        assertThat(result.contentChunks().get(1)).contains("Lines 3-3", "Line 3: three");
    }

    @Test
    void detectsCsvDelimiterAndRepeatsHeaderForNextChunk() {
        ExtractedFileData result = extractor.extract("events.csv", null,
                "name;date\nCup;2026-01-01\nRace;2026-02-01".getBytes(StandardCharsets.UTF_8));

        assertThat(result.contentChunks()).hasSize(2);
        assertThat(result.contentChunks().getFirst()).contains("Row 1: name | date", "Row 2: Cup | 2026-01-01");
        assertThat(result.contentChunks().get(1)).contains("Possible header: Row 1: name | date", "Row 3: Race");
    }

    @Test
    void extractsNonEmptyExcelRowsBySheet() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Season");
            sheet.createRow(0).createCell(0).setCellValue("Title");
            sheet.createRow(1).createCell(0).setCellValue("Cup");
            workbook.write(output);
            bytes = output.toByteArray();
        }

        ExtractedFileData result = extractor.extract("events.xlsx", null, bytes);

        assertThat(result.contentChunks()).singleElement().asString()
                .contains("Sheet: Season", "Row 1: Title", "Row 2: Cup");
    }

    @Test
    void rejectsUnsupportedAndBrokenFiles() {
        assertThatThrownBy(() -> extractor.extract("events.bin", "application/octet-stream", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported import file type");
        assertThatThrownBy(() -> extractor.extract("events.xlsx", null, new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("failed to read Excel file");
    }
}
