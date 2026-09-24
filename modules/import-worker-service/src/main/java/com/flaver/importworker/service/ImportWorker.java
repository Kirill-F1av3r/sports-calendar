package com.flaver.importworker.service;

import com.flaver.dto.importing.ImportRequestedEvent;
import com.flaver.importworker.ai.AiImportClient;
import com.flaver.importworker.ai.ImportedEvents;
import com.flaver.importworker.config.ImportProcessingProperties;
import com.flaver.importworker.dto.CompleteImportRequest;
import com.flaver.importworker.dto.ProcessingImportResponse;
import com.flaver.importworker.extract.ExtractedFileData;
import com.flaver.importworker.extract.ExtractedImageChunk;
import com.flaver.importworker.extract.FileDataExtractor;
import com.flaver.importworker.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ImportWorker {
    private static final Logger log = LoggerFactory.getLogger(ImportWorker.class);

    private final ImportServiceClient importServiceClient;
    private final ObjectStorage objectStorage;
    private final FileDataExtractor fileDataExtractor;
    private final AiImportClient aiImportClient;
    private final ImportProcessingProperties processingProperties;

    public ImportWorker(ImportServiceClient importServiceClient,
                        ObjectStorage objectStorage,
                        FileDataExtractor fileDataExtractor,
                        AiImportClient aiImportClient,
                        ImportProcessingProperties processingProperties) {
        this.importServiceClient = importServiceClient;
        this.objectStorage = objectStorage;
        this.fileDataExtractor = fileDataExtractor;
        this.aiImportClient = aiImportClient;
        this.processingProperties = processingProperties;
    }

    @KafkaListener(topics = "import.jobs.requested", containerFactory = "kafkaListenerContainerFactory")
    public void handle(ImportRequestedEvent event) {
        try {
            log.info("Starting import job {} for file {}", event.jobId(), event.fileName());
            ProcessingImportResponse processing = importServiceClient.markProcessing(event.jobId());
            if (processing == null || !processing.accepted()) {
                log.info("Skipping import job {} because current status is {}", event.jobId(),
                        processing != null ? processing.status() : "unknown");
                return;
            }
            byte[] fileBytes = objectStorage.get(event.objectKey());
            ExtractedFileData extracted = fileDataExtractor.extract(event.fileName(), event.contentType(), fileBytes);

            ImportedEvents importedEvents = extractFromChunks(event, extracted);

            importServiceClient.markComplete(event.jobId(), new CompleteImportRequest(importedEvents.events()));
            log.info("Import job {} completed. Imported draft events: {}", event.jobId(), importedEvents.events().size());
        } catch (Exception ex) {
            log.warn("Import job {} failed: {}", event.jobId(), ex.getMessage());
            importServiceClient.markFailed(event.jobId(), ex.getMessage());
        }
    }

    private ImportedEvents extractFromChunks(ImportRequestedEvent event, ExtractedFileData extracted) {
        List<CompleteImportRequest.ImportedDraftEventRequest> allEvents = new ArrayList<>();
        allEvents.addAll(extractFromTextChunks(event, extracted.contentChunks()).events());
        allEvents.addAll(extractFromImageChunks(event, extracted.imageChunks()).events());

        if (allEvents.isEmpty() && extracted.contentChunks().isEmpty() && extracted.imageChunks().isEmpty()) {
            throw new IllegalArgumentException("file does not contain extractable data");
        }
        return new ImportedEvents(deduplicate(allEvents));
    }

    private ImportedEvents extractFromTextChunks(ImportRequestedEvent event, List<String> chunks) {
        List<String> normalizedChunks = chunks == null ? List.of() : chunks;
        List<CompleteImportRequest.ImportedDraftEventRequest> allEvents = new ArrayList<>();

        for (int index = 0; index < normalizedChunks.size(); index++) {
            log.info("Import job {}: processing text chunk {}/{}", event.jobId(), index + 1, normalizedChunks.size());
            ImportedEvents chunkEvents = extractTextChunkWithFallback(
                    event,
                    normalizedChunks.get(index),
                    "text chunk " + (index + 1) + "/" + normalizedChunks.size(),
                    0
            );
            allEvents.addAll(chunkEvents.events());
            log.info("Import job {}: text chunk {}/{} returned {} events",
                    event.jobId(), index + 1, normalizedChunks.size(), chunkEvents.events().size());
        }

        return new ImportedEvents(deduplicate(allEvents));
    }

    private ImportedEvents extractTextChunkWithFallback(ImportRequestedEvent event,
                                                        String chunkData,
                                                        String chunkLabel,
                                                        int depth) {
        try {
            String prompt = buildChunkPromptPart(event, chunkData, chunkLabel);
            return aiImportClient.extractFromText(event, prompt);
        } catch (RuntimeException ex) {
            List<String> smallerChunks = splitFailedTextChunk(chunkData);
            if (smallerChunks.size() <= 1) {
                throw ex;
            }

            log.warn("Import job {}: {} failed: {}. Retrying as {} smaller chunks",
                    event.jobId(), chunkLabel, ex.getMessage(), smallerChunks.size());

            List<CompleteImportRequest.ImportedDraftEventRequest> allEvents = new ArrayList<>();
            for (int index = 0; index < smallerChunks.size(); index++) {
                ImportedEvents chunkEvents = extractTextChunkWithFallback(
                        event,
                        smallerChunks.get(index),
                        chunkLabel + "." + (index + 1) + "/" + smallerChunks.size(),
                        depth + 1
                );
                allEvents.addAll(chunkEvents.events());
            }
            return new ImportedEvents(deduplicate(allEvents));
        }
    }

    private ImportedEvents extractFromImageChunks(ImportRequestedEvent event, List<ExtractedImageChunk> chunks) {
        List<ExtractedImageChunk> normalizedChunks = chunks == null ? List.of() : chunks;
        List<CompleteImportRequest.ImportedDraftEventRequest> allEvents = new ArrayList<>();

        for (int index = 0; index < normalizedChunks.size(); index++) {
            log.info("Import job {}: processing image chunk {}/{}", event.jobId(), index + 1, normalizedChunks.size());
            ExtractedImageChunk chunk = normalizedChunks.get(index);
            String sourceDescription = buildImageChunkDescription(event, chunk, index + 1, normalizedChunks.size());
            ImportedEvents chunkEvents = aiImportClient.extractFromImage(event, chunk.content(), sourceDescription);
            allEvents.addAll(chunkEvents.events());
            log.info("Import job {}: image chunk {}/{} returned {} events",
                    event.jobId(), index + 1, normalizedChunks.size(), chunkEvents.events().size());
        }

        return new ImportedEvents(deduplicate(allEvents));
    }

    private String buildChunkPromptPart(ImportRequestedEvent event, String chunk, String chunkLabel) {
        return """
                Source file: %s
                Part: %s.
                Extract all sports competition events from this part.
                Do not skip rows that contain competitions.
                Source references may use row numbers from this part.

                %s
                """.formatted(event.fileName(), chunkLabel, chunk);
    }

    private List<String> splitFailedTextChunk(String chunkData) {
        List<String> lines = chunkData == null
                ? List.of()
                : chunkData.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        int lineCount = processingProperties.normalizedFailedTextChunkLineCount();
        if (lines.size() <= lineCount) {
            return List.of(chunkData);
        }

        List<String> contextLines = lines.stream()
                .filter(line -> !isDataLine(line))
                .toList();
        List<String> dataLines = lines.stream()
                .filter(this::isDataLine)
                .toList();
        if (dataLines.isEmpty()) {
            dataLines = lines;
            contextLines = List.of();
        }

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < dataLines.size(); start += lineCount) {
            int end = Math.min(start + lineCount, dataLines.size());
            StringBuilder chunk = new StringBuilder();
            for (String contextLine : contextLines) {
                chunk.append(contextLine).append("\n");
            }
            for (int index = start; index < end; index++) {
                chunk.append(dataLines.get(index)).append("\n");
            }
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    private boolean isDataLine(String line) {
        return line.startsWith("Line ") || line.startsWith("Row ");
    }

    private String buildImageChunkDescription(ImportRequestedEvent event,
                                              ExtractedImageChunk chunk,
                                              int chunkNumber,
                                              int chunkCount) {
        String sourceDescription = chunk.sourceDescription() == null ? "image" : chunk.sourceDescription();
        return "%s. Source file: %s. Image part %d of %d."
                .formatted(sourceDescription, event.fileName(), chunkNumber, chunkCount);
    }

    private List<CompleteImportRequest.ImportedDraftEventRequest> deduplicate(
            List<CompleteImportRequest.ImportedDraftEventRequest> events
    ) {
        Map<String, CompleteImportRequest.ImportedDraftEventRequest> unique = new LinkedHashMap<>();
        for (CompleteImportRequest.ImportedDraftEventRequest event : events) {
            String key = deduplicationKey(event);
            CompleteImportRequest.ImportedDraftEventRequest existing = unique.get(key);
            if (existing == null || eventScore(event) > eventScore(existing)) {
                unique.put(key, event);
            }
        }
        return List.copyOf(unique.values());
    }

    private String deduplicationKey(CompleteImportRequest.ImportedDraftEventRequest event) {
        String title = normalize(event.title());
        String location = normalize(event.location());
        String startDate = dateKey(event.startDate());
        String endDate = dateKey(event.endDate());
        if (title.isBlank() && startDate.isBlank() && endDate.isBlank()) {
            return "raw|" + normalize(event.rawText()) + "|" + normalize(event.sourceReference());
        }
        return title + "|" + startDate + "|" + endDate + "|" + location;
    }

    private int eventScore(CompleteImportRequest.ImportedDraftEventRequest event) {
        int score = 0;
        score += notBlank(event.title()) ? 3 : 0;
        score += event.startDate() != null ? 3 : 0;
        score += event.endDate() != null ? 3 : 0;
        score += notBlank(event.competitionLevel()) ? 1 : 0;
        score += notBlank(event.location()) ? 1 : 0;
        score += notBlank(event.externalUrl()) ? 1 : 0;
        score += event.disciplines() == null ? 0 : event.disciplines().size();
        score += notBlank(event.priority()) ? 1 : 0;
        score += notBlank(event.sourceReference()) ? 1 : 0;
        score += notBlank(event.rawText()) ? 1 : 0;
        return score;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String dateKey(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
