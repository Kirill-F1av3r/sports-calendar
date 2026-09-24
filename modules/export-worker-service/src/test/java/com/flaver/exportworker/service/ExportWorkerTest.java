package com.flaver.exportworker.service;

import com.flaver.dto.export.CalendarExportData;
import com.flaver.dto.export.ExportRequestedEvent;
import com.flaver.dto.integration.GoogleAccessTokenResponse;
import com.flaver.exportworker.dto.CompleteExportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportWorkerTest {
    @Mock CalendarClient calendarClient;
    @Mock ExportServiceClient exportServiceClient;
    @Mock IntegrationClient integrationClient;
    @Mock GoogleSheetsClient googleSheetsClient;

    private ExportWorker worker;
    private ExportRequestedEvent event;

    @BeforeEach
    void setUp() {
        worker = new ExportWorker(calendarClient, exportServiceClient, integrationClient, googleSheetsClient);
        event = new ExportRequestedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "GOOGLE_SHEETS");
    }

    @Test
    void completesGoogleSheetsExport() {
        CalendarExportData data = new CalendarExportData(event.calendarId(), "Calendar", "Ski", 2026, List.of());
        when(calendarClient.getExportData(event.calendarId(), event.userId())).thenReturn(data);
        when(integrationClient.getGoogleAccessToken(event.userId()))
                .thenReturn(new GoogleAccessTokenResponse("access", 3600L, "scope"));
        when(googleSheetsClient.export(data, "access"))
                .thenReturn(new GoogleSheetsClient.GoogleSpreadsheetResult("sheet", "https://sheet"));

        worker.handle(event);

        verify(exportServiceClient).markProcessing(event.jobId());
        verify(exportServiceClient).markSuccess(event.jobId(), new CompleteExportRequest("sheet", "https://sheet"));
        verify(exportServiceClient, never()).markFailed(any(), any());
    }

    @Test
    void reportsUnsupportedProviderAsFailure() {
        ExportRequestedEvent unsupported = new ExportRequestedEvent(
                event.jobId(), event.userId(), event.calendarId(), "CSV");
        when(calendarClient.getExportData(unsupported.calendarId(), unsupported.userId()))
                .thenReturn(new CalendarExportData(unsupported.calendarId(), "Calendar", "Ski", 2026, List.of()));

        worker.handle(unsupported);

        verify(exportServiceClient).markFailed(org.mockito.ArgumentMatchers.eq(unsupported.jobId()), contains("Unsupported"));
        verify(integrationClient, never()).getGoogleAccessToken(any());
    }

    @Test
    void reportsDependencyFailure() {
        when(calendarClient.getExportData(event.calendarId(), event.userId()))
                .thenThrow(new IllegalStateException("calendar unavailable"));

        worker.handle(event);

        verify(exportServiceClient).markFailed(event.jobId(), "calendar unavailable");
        verify(exportServiceClient, never()).markSuccess(any(), any());
    }
}
