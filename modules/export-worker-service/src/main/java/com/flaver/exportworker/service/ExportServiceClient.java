package com.flaver.exportworker.service;

import com.flaver.exportworker.config.WorkerServicesProperties;
import com.flaver.exportworker.dto.CompleteExportRequest;
import com.flaver.exportworker.dto.FailExportRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ExportServiceClient {
    private final RestClient restClient;
    private final WorkerServicesProperties properties;

    public ExportServiceClient(RestClient.Builder builder, WorkerServicesProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public void markProcessing(UUID jobId) {
        restClient.post()
                .uri(properties.export().url() + "/internal/exports/" + jobId + "/processing")
                .retrieve()
                .toBodilessEntity();
    }

    public void markSuccess(UUID jobId, CompleteExportRequest request) {
        restClient.post()
                .uri(properties.export().url() + "/internal/exports/" + jobId + "/success")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void markFailed(UUID jobId, String errorMessage) {
        restClient.post()
                .uri(properties.export().url() + "/internal/exports/" + jobId + "/failed")
                .body(new FailExportRequest(errorMessage))
                .retrieve()
                .toBodilessEntity();
    }
}
