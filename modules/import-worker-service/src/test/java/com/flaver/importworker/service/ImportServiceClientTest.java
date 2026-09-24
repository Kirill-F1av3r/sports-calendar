package com.flaver.importworker.service;

import com.flaver.importworker.config.WorkerServicesProperties;
import com.flaver.importworker.dto.CompleteImportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ImportServiceClientTest {
    private MockRestServiceServer server;
    private ImportServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ImportServiceClient(builder, new WorkerServicesProperties(
                new WorkerServicesProperties.Service("http://import")));
    }

    @Test
    void sendsCompleteLifecycle() {
        UUID jobId = UUID.randomUUID();
        server.expect(requestTo("http://import/internal/imports/" + jobId + "/processing"))
                .andRespond(withSuccess("{\"accepted\":true,\"status\":\"PROCESSING\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://import/internal/imports/" + jobId + "/complete"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://import/internal/imports/" + jobId + "/failed"))
                .andRespond(withSuccess());

        assertThat(client.markProcessing(jobId).accepted()).isTrue();
        client.markComplete(jobId, new CompleteImportRequest(List.of()));
        client.markFailed(jobId, null);

        server.verify();
    }
}
