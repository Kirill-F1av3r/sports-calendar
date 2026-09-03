package com.flaver.exportworker.service;

import com.flaver.dto.integration.GoogleAccessTokenRequest;
import com.flaver.dto.integration.GoogleAccessTokenResponse;
import com.flaver.exportworker.config.WorkerServicesProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class IntegrationClient {
    private final RestClient restClient;
    private final WorkerServicesProperties properties;

    public IntegrationClient(RestClient.Builder builder, WorkerServicesProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public GoogleAccessTokenResponse getGoogleAccessToken(UUID userId) {
        return restClient.post()
                .uri(properties.integration().url() + "/internal/integrations/google/access-token")
                .body(new GoogleAccessTokenRequest(userId))
                .retrieve()
                .body(GoogleAccessTokenResponse.class);
    }
}
