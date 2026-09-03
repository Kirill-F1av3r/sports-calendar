package com.flaver.exportworker.service;

import com.flaver.dto.export.CalendarExportData;
import com.flaver.dto.export.ExportRequestedEvent;
import com.flaver.dto.integration.GoogleAccessTokenResponse;
import com.flaver.exportworker.dto.CompleteExportRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExportWorker {
    private final CalendarClient calendarClient;
    private final ExportServiceClient exportServiceClient;
    private final IntegrationClient integrationClient;
    private final GoogleSheetsClient googleSheetsClient;

    public ExportWorker(CalendarClient calendarClient,
                        ExportServiceClient exportServiceClient,
                        IntegrationClient integrationClient,
                        GoogleSheetsClient googleSheetsClient) {
        this.calendarClient = calendarClient;
        this.exportServiceClient = exportServiceClient;
        this.integrationClient = integrationClient;
        this.googleSheetsClient = googleSheetsClient;
    }

    @KafkaListener(topics = "export.jobs.requested", containerFactory = "kafkaListenerContainerFactory")
    public void handle(ExportRequestedEvent event) {
        try {
            exportServiceClient.markProcessing(event.jobId());
            CalendarExportData data = calendarClient.getExportData(event.calendarId(), event.userId());
            if (!"GOOGLE_SHEETS".equalsIgnoreCase(event.provider())) {
                throw new IllegalArgumentException("Unsupported export provider: " + event.provider());
            }

            GoogleAccessTokenResponse token = integrationClient.getGoogleAccessToken(event.userId());
            GoogleSheetsClient.GoogleSpreadsheetResult result = googleSheetsClient.export(data, token.accessToken());
            exportServiceClient.markSuccess(event.jobId(),
                    new CompleteExportRequest(result.spreadsheetId(), result.spreadsheetUrl()));
        } catch (Exception ex) {
            exportServiceClient.markFailed(event.jobId(), ex.getMessage());
        }
    }
}
