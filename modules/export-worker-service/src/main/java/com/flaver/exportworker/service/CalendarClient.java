package com.flaver.exportworker.service;

import com.flaver.dto.export.CalendarExportData;
import com.flaver.exportworker.config.WorkerServicesProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CalendarClient {
    private final RestClient restClient;
    private final WorkerServicesProperties properties;

    public CalendarClient(RestClient.Builder builder, WorkerServicesProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public CalendarExportData getExportData(UUID calendarId, UUID userId) {
        return restClient.get()
                .uri(properties.calendar().url() + "/calendars/internal/" + calendarId + "/export-data")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(CalendarExportData.class);
    }
}
