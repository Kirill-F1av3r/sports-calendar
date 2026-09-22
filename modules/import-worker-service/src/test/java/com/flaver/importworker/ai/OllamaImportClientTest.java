package com.flaver.importworker.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaver.dto.importing.ImportRequestedEvent;
import com.flaver.importworker.config.OllamaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaImportClientTest {
    private MockRestServiceServer server;
    private OllamaImportClient client;
    private final ImportRequestedEvent event = new ImportRequestedEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
            null, "object", "image.png", "image/png");

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OllamaImportClient(new OllamaProperties(
                "http://ollama.test", "text-model", "vision-model", "5m", 5, 4096, 1024),
                new ObjectMapper(), builder.build());
    }

    @Test
    void parsesJsonFromMarkdownResponse() throws Exception {
        expectChat("text-model", "```json\n" + validEventsJson() + "\n```");

        ImportedEvents result = client.extractFromText(event, "source");

        assertThat(result.events()).singleElement().satisfies(imported -> {
            assertThat(imported.title()).isEqualTo("Cup");
            assertThat(imported.location()).isNull();
            assertThat(imported.disciplines()).containsExactly("Sprint");
        });
    }

    @Test
    void repairsFirstInvalidResponse() throws Exception {
        expectChat("text-model", "{\"events\":[{\"name\":\"Cup\"}]}");
        expectChat("text-model", validEventsJson());

        assertThat(client.extractFromText(event, "source").events()).hasSize(1);
        server.verify();
    }

    @Test
    void sendsImagesToVisionModel() throws Exception {
        server.expect(requestTo("http://ollama.test/api/chat"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("vision-model"),
                        org.hamcrest.Matchers.containsString("AQID"))))
                .andRespond(withSuccess(chatResponse("{\"events\":[]}"), MediaType.APPLICATION_JSON));

        assertThat(client.extractFromImage(event, new byte[]{1, 2, 3}, "page 1").events()).isEmpty();
    }

    @Test
    void rejectsEmptyAndRepeatedInvalidResponses() throws Exception {
        expectChat("text-model", "not json");
        expectChat("text-model", "still not json");

        assertThatThrownBy(() -> client.extractFromText(event, "source"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI returned invalid events JSON");
    }

    private void expectChat(String model, String content) throws Exception {
        server.expect(requestTo("http://ollama.test/api/chat"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(model)))
                .andRespond(withSuccess(chatResponse(content), MediaType.APPLICATION_JSON));
    }

    private String chatResponse(String content) throws Exception {
        return new ObjectMapper().writeValueAsString(java.util.Map.of("message", java.util.Map.of("content", content)));
    }

    private String validEventsJson() {
        return """
                {"events":[{"title":"Cup","startDate":"2026-01-02","endDate":"2026-01-02",
                "competitionLevel":"NATIONAL","location":null,"externalUrl":null,
                "disciplines":["Sprint"],"priority":null,"sourceReference":"Row 1","rawText":"row"}]}
                """;
    }
}
