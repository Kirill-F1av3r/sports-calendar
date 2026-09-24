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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportWorkerTest {
    @Mock ImportServiceClient importServiceClient;
    @Mock ObjectStorage objectStorage;
    @Mock FileDataExtractor fileDataExtractor;
    @Mock AiImportClient aiImportClient;

    private ImportWorker worker;
    private ImportRequestedEvent event;

    @BeforeEach
    void setUp() {
        worker = new ImportWorker(importServiceClient, objectStorage, fileDataExtractor, aiImportClient,
                new ImportProcessingProperties(20, 20, 2, 20, 120));
        event = new ImportRequestedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2026,
                "Ski", "imports/file", "events.csv", "text/csv");
    }

    @Test
    void skipsJobNotAcceptedForProcessing() {
        when(importServiceClient.markProcessing(event.jobId()))
                .thenReturn(new ProcessingImportResponse(false, "SUCCESS"));

        worker.handle(event);

        verify(objectStorage, never()).get(anyString());
        verify(importServiceClient, never()).markComplete(any(), any());
    }

    @Test
    void extractsTextAndImagesThenKeepsRicherDuplicate() {
        byte[] bytes = {1, 2};
        ExtractedImageChunk image = new ExtractedImageChunk("page 2", new byte[]{3});
        CompleteImportRequest.ImportedDraftEventRequest sparse = draft(" Cup ", null, List.of());
        CompleteImportRequest.ImportedDraftEventRequest rich = draft("cup", "https://event", List.of("Sprint"));
        when(importServiceClient.markProcessing(event.jobId())).thenReturn(new ProcessingImportResponse(true, "PROCESSING"));
        when(objectStorage.get(event.objectKey())).thenReturn(bytes);
        when(fileDataExtractor.extract(event.fileName(), event.contentType(), bytes))
                .thenReturn(ExtractedFileData.mixed(List.of("Row 1: Cup"), List.of(image)));
        when(aiImportClient.extractFromText(org.mockito.ArgumentMatchers.eq(event), contains("Row 1: Cup")))
                .thenReturn(new ImportedEvents(List.of(sparse)));
        when(aiImportClient.extractFromImage(event, image.content(), "page 2. Source file: events.csv. Image part 1 of 1."))
                .thenReturn(new ImportedEvents(List.of(rich)));

        worker.handle(event);

        ArgumentCaptor<CompleteImportRequest> captor = ArgumentCaptor.forClass(CompleteImportRequest.class);
        verify(importServiceClient).markComplete(org.mockito.ArgumentMatchers.eq(event.jobId()), captor.capture());
        assertThat(captor.getValue().events()).containsExactly(rich);
        verify(importServiceClient, never()).markFailed(any(), any());
    }

    @Test
    void splitsFailedTextChunkAndRetriesSmallerParts() {
        String chunk = "CSV file\nRow 1: header\nRow 2: first\nRow 3: second\nRow 4: third";
        when(importServiceClient.markProcessing(event.jobId())).thenReturn(new ProcessingImportResponse(true, "PROCESSING"));
        when(objectStorage.get(event.objectKey())).thenReturn(new byte[0]);
        when(fileDataExtractor.extract(anyString(), anyString(), any())).thenReturn(ExtractedFileData.text(chunk));
        when(aiImportClient.extractFromText(org.mockito.ArgumentMatchers.eq(event), anyString()))
                .thenThrow(new IllegalStateException("too large"))
                .thenReturn(new ImportedEvents(List.of(draft("First", null, List.of()))))
                .thenReturn(new ImportedEvents(List.of(draft("Second", null, List.of()))));

        worker.handle(event);

        ArgumentCaptor<CompleteImportRequest> captor = ArgumentCaptor.forClass(CompleteImportRequest.class);
        verify(importServiceClient).markComplete(org.mockito.ArgumentMatchers.eq(event.jobId()), captor.capture());
        assertThat(captor.getValue().events()).hasSize(2);
    }

    @Test
    void reportsEmptyExtractedFileAndNullProcessingResponse() {
        when(importServiceClient.markProcessing(event.jobId())).thenReturn(new ProcessingImportResponse(true, "PROCESSING"));
        when(objectStorage.get(event.objectKey())).thenReturn(new byte[0]);
        when(fileDataExtractor.extract(anyString(), anyString(), any())).thenReturn(ExtractedFileData.textChunks(List.of()));

        worker.handle(event);

        verify(importServiceClient).markFailed(event.jobId(), "file does not contain extractable data");
        verify(importServiceClient, never()).markComplete(any(), any());
    }

    private CompleteImportRequest.ImportedDraftEventRequest draft(String title, String url, List<String> disciplines) {
        return new CompleteImportRequest.ImportedDraftEventRequest(title, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2), "NATIONAL", "Moscow", url, disciplines,
                "IMPORTANT", "row", "raw");
    }
}
