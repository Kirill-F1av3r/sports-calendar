package com.flaver.importworker.service;

import com.flaver.importworker.config.WorkerServicesProperties;
import com.flaver.importworker.dto.CompleteImportRequest;
import com.flaver.importworker.dto.FailImportRequest;
import com.flaver.importworker.dto.ProcessingImportResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ImportServiceClient {
    private final RestClient restClient;
    private final WorkerServicesProperties properties;

    public ImportServiceClient(RestClient.Builder builder, WorkerServicesProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public ProcessingImportResponse markProcessing(UUID jobId) {
        return restClient.post()
                .uri(properties.importService().url() + "/internal/imports/" + jobId + "/processing")
                .retrieve()
                .body(ProcessingImportResponse.class);
    }

    public void markComplete(UUID jobId, CompleteImportRequest request) {
        restClient.post()
                .uri(properties.importService().url() + "/internal/imports/" + jobId + "/complete")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void markFailed(UUID jobId, String errorMessage) {
        restClient.post()
                .uri(properties.importService().url() + "/internal/imports/" + jobId + "/failed")
                .body(new FailImportRequest(errorMessage != null ? errorMessage : "Import failed"))
                .retrieve()
                .toBodilessEntity();
    }
}
