package com.flaver.importworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.ollama")
public record OllamaProperties(
        String baseUrl,
        String textModel,
        String visionModel,
        String keepAlive,
        int requestTimeoutSeconds,
        int numCtx,
        int numPredict
) {
}
