package com.flaver.exportservice.service;

import com.flaver.exportservice.config.ExportServiceProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CalendarClient {
    private final RestClient restClient;
    private final ExportServiceProperties properties;

    public CalendarClient(RestClient.Builder builder, ExportServiceProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public void checkAccess(UUID calendarId, UUID userId) {
        restClient.get()
                .uri(properties.url() + "/calendars/internal/" + calendarId + "/access-check")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .toBodilessEntity();
    }
}
