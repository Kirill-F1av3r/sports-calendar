package com.flaver.importservice.service;

import com.flaver.dto.calendar.BatchCreateEventsRequest;
import com.flaver.dto.calendar.BatchCreateEventsResponse;
import com.flaver.importservice.config.ImportServicesProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CalendarClient {
    private final RestClient restClient;
    private final ImportServicesProperties properties;

    public CalendarClient(RestClient.Builder builder, ImportServicesProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public void checkAccess(UUID calendarId, UUID userId) {
        restClient.get()
                .uri(properties.calendar().url() + "/calendars/internal/" + calendarId + "/access-check")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .toBodilessEntity();
    }

    public CalendarInfo getCalendar(UUID calendarId, UUID userId) {
        return restClient.get()
                .uri(properties.calendar().url() + "/calendars/" + calendarId)
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(CalendarInfo.class);
    }

    public int createEvents(UUID calendarId, UUID userId, BatchCreateEventsRequest request) {
        BatchCreateEventsResponse response = restClient.post()
                .uri(properties.calendar().url() + "/calendars/internal/" + calendarId + "/events/batch")
                .header("X-User-Id", userId.toString())
                .body(request)
                .retrieve()
                .body(BatchCreateEventsResponse.class);
        return response != null ? response.createdEvents() : 0;
    }

    public record CalendarInfo(UUID id, String name, String sportType, Integer year) {
    }
}
