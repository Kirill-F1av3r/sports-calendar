package com.flaver.importworker.extract;

import com.flaver.importworker.config.ImportProcessingProperties;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.imageio.ImageIO;

@Component
public class DefaultFileDataExtractor implements FileDataExtractor {
    private final ImportProcessingProperties properties;

    public DefaultFileDataExtractor(ImportProcessingProperties properties) {
        this.properties = properties;
    }

    @Override
    public ExtractedFileData extract(String fileName, String contentType, byte[] bytes) {
        if (isImage(fileName, contentType)) {
            return ExtractedFileData.image(bytes, "Uploaded image: " + fileName);
        }
        if (isExcel(fileName, contentType)) {
            return ExtractedFileData.textChunks(extractExcelChunks(bytes));
        }
        if (isPdf(fileName, contentType)) {
            return extractPdf(bytes);
        }
        if (isCsv(fileName, contentType)) {
            return ExtractedFileData.textChunks(extractCsvChunks(bytes));
        }
        if (isText(fileName, contentType)) {
            return ExtractedFileData.textChunks(chunkTextLines("Text file: " + fileName, new String(bytes, StandardCharsets.UTF_8)));
        }
        throw new IllegalArgumentException("unsupported import file type");
    }

    private List<String> extractExcelChunks(byte[] bytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("ru-RU"));
            List<String> chunks = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                List<TableRowText> rows = new ArrayList<>();
                for (Row row : sheet) {
                    String values = formatExcelRow(row, formatter);
                    if (!values.isBlank()) {
                        rows.add(new TableRowText(row.getRowNum() + 1, "Row " + (row.getRowNum() + 1) + ": " + values));
                    }
                }
                chunks.addAll(chunkTableRows("Sheet: " + sheet.getSheetName(), rows));
            }
            return chunks;
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read Excel file", ex);
        }
    }

    private List<String> extractCsvChunks(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        try (CSVParser parser = CSVParser.parse(new StringReader(text), CSVFormat.DEFAULT.builder()
                .setDelimiter(detectDelimiter(text))
                .setIgnoreEmptyLines(true)
                .build())) {
            List<TableRowText> rows = new ArrayList<>();
            int rowNumber = 1;
            for (var record : parser) {
                String values = StreamSupport.stream(record.spliterator(), false)
                        .map(String::trim)
                        .collect(Collectors.joining(" | "));
                if (!values.isBlank()) {
                    rows.add(new TableRowText(rowNumber, "Row " + rowNumber + ": " + values));
                }
                rowNumber++;
            }
            return chunkTableRows("CSV file", rows);
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read CSV file", ex);
        }
    }

    private String formatExcelRow(Row row, DataFormatter formatter) {
        short firstCellNumber = row.getFirstCellNum();
        short lastCellNumber = row.getLastCellNum();
        if (firstCellNumber < 0 || lastCellNumber < 0) {
            return "";
        }

        List<String> values = new ArrayList<>();
        for (int cellIndex = firstCellNumber; cellIndex < lastCellNumber; cellIndex++) {
            Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
        }
        return String.join(" | ", values).trim();
    }

    private List<String> chunkTableRows(String sourceName, List<TableRowText> rows) {
        if (rows.isEmpty()) {
            return List.of(sourceName + "\nNo non-empty rows were found.");
        }

        int chunkSize = properties.normalizedTableChunkRowCount();
        List<String> chunks = new ArrayList<>();
        String possibleHeader = rows.getFirst().text();
        for (int start = 0; start < rows.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, rows.size());
            List<TableRowText> chunkRows = rows.subList(start, end);

            StringBuilder chunk = new StringBuilder();
            chunk.append(sourceName).append("\n");
            chunk.append("Rows ").append(chunkRows.getFirst().rowNumber())
                    .append("-").append(chunkRows.getLast().rowNumber()).append("\n");
            if (start > 0) {
                chunk.append("Possible header: ").append(possibleHeader).append("\n");
            }
            for (TableRowText row : chunkRows) {
                chunk.append(row.text()).append("\n");
            }
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    private ExtractedFileData extractPdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            PDFRenderer renderer = new PDFRenderer(document);
            List<String> textChunks = new ArrayList<>();
            List<ExtractedImageChunk> imageChunks = new ArrayList<>();

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                if (isUsefulPdfText(pageText)) {
                    textChunks.add(formatPdfPageTextChunk(page, pageText));
                } else {
                    imageChunks.add(renderPdfPage(renderer, page));
                }
            }

            if (textChunks.isEmpty() && imageChunks.isEmpty()) {
                throw new IllegalArgumentException("PDF file does not contain readable text or renderable pages");
            }
            return ExtractedFileData.mixed(textChunks, imageChunks);
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read PDF file", ex);
        }
    }

    private boolean isUsefulPdfText(String text) {
        return text != null && text.replaceAll("\\s+", "").length() >= properties.normalizedPdfMinTextChars();
    }

    private String formatPdfPageTextChunk(int pageNumber, String pageText) {
        StringBuilder chunk = new StringBuilder();
        chunk.append("PDF page ").append(pageNumber).append("\n");
        List<String> lines = pageText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        for (int index = 0; index < lines.size(); index++) {
            chunk.append("Line ").append(index + 1).append(": ").append(lines.get(index)).append("\n");
        }
        return chunk.toString();
    }

    private ExtractedImageChunk renderPdfPage(PDFRenderer renderer, int pageNumber) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, properties.normalizedPdfImageDpi(), ImageType.RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return new ExtractedImageChunk("PDF page " + pageNumber + " rendered as image", output.toByteArray());
        }
    }

    private List<String> chunkTextLines(String sourceName, String text) {
        List<String> lines = text == null
                ? List.of()
                : text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return List.of();
        }

        int chunkSize = properties.normalizedTextChunkLineCount();
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, lines.size());
            StringBuilder chunk = new StringBuilder();
            chunk.append(sourceName).append("\n");
            chunk.append("Lines ").append(start + 1).append("-").append(end).append("\n");
            for (int index = start; index < end; index++) {
                chunk.append("Line ").append(index + 1).append(": ").append(lines.get(index)).append("\n");
            }
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    private char detectDelimiter(String text) {
        long semicolons = text.chars().filter(ch -> ch == ';').count();
        long commas = text.chars().filter(ch -> ch == ',').count();
        return semicolons > commas ? ';' : ',';
    }

    private boolean isExcel(String fileName, String contentType) {
        return hasExtension(fileName, ".xlsx", ".xls")
                || matches(contentType, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || matches(contentType, "application/vnd.ms-excel");
    }

    private boolean isCsv(String fileName, String contentType) {
        return hasExtension(fileName, ".csv")
                || matches(contentType, "text/csv")
                || matches(contentType, "application/csv");
    }

    private boolean isPdf(String fileName, String contentType) {
        return hasExtension(fileName, ".pdf") || matches(contentType, "application/pdf");
    }

    private boolean isText(String fileName, String contentType) {
        return hasExtension(fileName, ".txt") || matches(contentType, "text/plain");
    }

    private boolean isImage(String fileName, String contentType) {
        return hasExtension(fileName, ".png", ".jpg", ".jpeg", ".webp")
                || matches(contentType, "image/png")
                || matches(contentType, "image/jpeg")
                || matches(contentType, "image/webp");
    }

    private boolean hasExtension(String fileName, String... extensions) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String actual, String expected) {
        return actual != null && actual.equalsIgnoreCase(expected);
    }

    private record TableRowText(int rowNumber, String text) {
    }
}
