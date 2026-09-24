package com.flaver.importservice.service;

import com.flaver.dto.calendar.BatchCreateEventsRequest;
import com.flaver.importservice.config.ImportServicesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CalendarClientTest {
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private CalendarClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CalendarClient(builder, new ImportServicesProperties(
                new ImportServicesProperties.Service("http://calendar")));
    }

    @Test
    void getsCalendarAndPassesUserIdentity() {
        UUID calendarId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        server.expect(requestTo("http://calendar/calendars/" + calendarId))
                .andExpect(header("X-User-Id", userId.toString()))
                .andRespond(withSuccess("{\"id\":\"" + calendarId + "\",\"name\":\"Season\",\"sportType\":\"Ski\",\"year\":2026}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.getCalendar(calendarId, userId).year()).isEqualTo(2026);
        server.verify();
    }

    @Test
    void returnsCreatedCountAndHandlesEmptyResponse() {
        UUID calendarId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String url = "http://calendar/calendars/internal/" + calendarId + "/events/batch";
        server.expect(requestTo(url)).andRespond(withSuccess("{\"createdEvents\":2}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(url)).andRespond(withSuccess());
        BatchCreateEventsRequest request = new BatchCreateEventsRequest(List.of());

        assertThat(client.createEvents(calendarId, userId, request)).isEqualTo(2);
        assertThat(client.createEvents(calendarId, userId, request)).isZero();
        server.verify();
    }
}
