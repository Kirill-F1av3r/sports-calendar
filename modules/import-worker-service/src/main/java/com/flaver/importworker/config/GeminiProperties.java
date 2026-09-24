package com.flaver.importworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String model,
        int requestTimeoutSeconds
) {
}
