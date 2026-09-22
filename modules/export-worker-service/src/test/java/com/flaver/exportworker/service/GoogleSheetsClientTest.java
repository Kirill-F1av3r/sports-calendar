package com.flaver.exportworker.service;

import com.flaver.dto.export.CalendarExportData;
import com.flaver.dto.export.CalendarExportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleSheetsClientTest {
    private MockRestServiceServer server;
    private GoogleSheetsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoogleSheetsClient(builder);
    }

    @Test
    void createsSpreadsheetAndWritesRows() {
        server.expect(once(), requestTo("https://sheets.googleapis.com/v4/spreadsheets"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access"))
                .andRespond(withSuccess("{\"spreadsheetId\":\"sheet-1\",\"spreadsheetUrl\":\"https://sheet-1\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/sheet-1/values/")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess());

        GoogleSheetsClient.GoogleSpreadsheetResult result = client.export(data(), "access");

        assertThat(result.spreadsheetId()).isEqualTo("sheet-1");
        assertThat(result.spreadsheetUrl()).isEqualTo("https://sheet-1");
        server.verify();
    }

    @Test
    void rejectsIncompleteCreateResponse() {
        server.expect(once(), requestTo("https://sheets.googleapis.com/v4/spreadsheets"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.export(data(), "access"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid create spreadsheet response");
    }

    private CalendarExportData data() {
        CalendarExportEvent event = new CalendarExportEvent(UUID.randomUUID(), "Cup",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2), "National", null,
                List.of("Sprint", " "), "Important", null, "Ski");
        return new CalendarExportData(UUID.randomUUID(), " Winter Cup ", "Ski", 2026, List.of(event));
    }
}
