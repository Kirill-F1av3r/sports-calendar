package com.flaver.importworker.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaver.importworker.config.GeminiProperties;
import com.flaver.importworker.config.OllamaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AiImportClientContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void createsGeminiClientWithTestConstructorPresent() {
        contextRunner
                .withPropertyValues("ai.provider=gemini")
                .withBean(GeminiProperties.class, () -> new GeminiProperties("key", "url", "model", 30))
                .withUserConfiguration(GeminiImportClient.class)
                .run(context -> assertThat(context).hasSingleBean(GeminiImportClient.class));
    }

    @Test
    void createsOllamaClientWithTestConstructorPresent() {
        contextRunner
                .withPropertyValues("ai.provider=ollama")
                .withBean(OllamaProperties.class, () -> new OllamaProperties(
                        "url", "text-model", "vision-model", "0", 30, 8192, 4096))
                .withUserConfiguration(OllamaImportClient.class)
                .run(context -> assertThat(context).hasSingleBean(OllamaImportClient.class));
    }
}
