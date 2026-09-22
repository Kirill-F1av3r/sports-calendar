package com.flaver.exportworker.service;

import com.flaver.exportworker.config.WorkerServicesProperties;
import com.flaver.exportworker.dto.CompleteExportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WorkerClientsTest {
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private WorkerServicesProperties properties;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        properties = new WorkerServicesProperties(
                new WorkerServicesProperties.Service("http://calendar"),
                new WorkerServicesProperties.Service("http://export"),
                new WorkerServicesProperties.Service("http://integration"));
    }

    @Test
    void calendarClientReadsExportData() {
        UUID calendarId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        server.expect(requestTo("http://calendar/calendars/internal/" + calendarId + "/export-data"))
                .andExpect(header("X-User-Id", userId.toString()))
                .andRespond(withSuccess("{\"calendarId\":\"" + calendarId + "\",\"calendarName\":\"Season\",\"events\":[]}",
                        MediaType.APPLICATION_JSON));

        assertThat(new CalendarClient(builder, properties).getExportData(calendarId, userId).calendarName())
                .isEqualTo("Season");
        server.verify();
    }

    @Test
    void integrationClientReadsAccessToken() {
        UUID userId = UUID.randomUUID();
        server.expect(requestTo("http://integration/internal/integrations/google/access-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"accessToken\":\"token\",\"expiresIn\":60,\"scopes\":\"sheets\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(new IntegrationClient(builder, properties).getGoogleAccessToken(userId).accessToken())
                .isEqualTo("token");
        server.verify();
    }

    @Test
    void exportClientSendsAllLifecycleCommands() {
        UUID jobId = UUID.randomUUID();
        server.expect(requestTo("http://export/internal/exports/" + jobId + "/processing"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://export/internal/exports/" + jobId + "/success"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://export/internal/exports/" + jobId + "/failed"))
                .andRespond(withSuccess());
        ExportServiceClient client = new ExportServiceClient(builder, properties);

        client.markProcessing(jobId);
        client.markSuccess(jobId, new CompleteExportRequest("sheet", "url"));
        client.markFailed(jobId, "failure");

        server.verify();
    }
}
