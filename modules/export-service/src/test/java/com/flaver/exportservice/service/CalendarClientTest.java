package com.flaver.exportservice.service;

import com.flaver.exportservice.config.ExportServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CalendarClientTest {
    @Test
    void checksCalendarAccessWithUserHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID calendarId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        server.expect(requestTo("http://calendar/calendars/internal/" + calendarId + "/access-check"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", userId.toString()))
                .andRespond(withSuccess());

        new CalendarClient(builder, new ExportServiceProperties("http://calendar"))
                .checkAccess(calendarId, userId);

        server.verify();
    }
}
