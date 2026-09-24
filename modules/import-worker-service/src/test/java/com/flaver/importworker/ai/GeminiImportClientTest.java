package com.flaver.importworker.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaver.dto.importing.ImportRequestedEvent;
import com.flaver.importworker.config.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiImportClientTest {
    private MockRestServiceServer server;
    private GeminiImportClient client;
    private final ImportRequestedEvent event = new ImportRequestedEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2026,
            "Ski", "object", "events.csv", "text/csv");

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiImportClient(properties("key", "gemini-test"), new ObjectMapper(), builder.build());
    }

    @Test
    void extractsStrictEventsFromText() throws Exception {
        server.expect(requestTo("https://gemini.test/v1beta/models/gemini-test:generateContent?key=key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Calendar year: 2026")))
                .andRespond(withSuccess(geminiResponse(validEventsJson()), MediaType.APPLICATION_JSON));

        ImportedEvents result = client.extractFromText(event, "Row 1: Cup");

        assertThat(result.events()).singleElement().satisfies(imported -> {
            assertThat(imported.title()).isEqualTo("Cup");
            assertThat(imported.startDate()).hasToString("2026-01-02");
            assertThat(imported.disciplines()).containsExactly("Sprint");
            assertThat(imported.sourceReference()).isEqualTo("12");
        });
        server.verify();
    }

    @Test
    void sendsImageWithDetectedMimeType() throws Exception {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(":generateContent")))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("image/jpeg"),
                        org.hamcrest.Matchers.containsString("/9gA"))))
                .andRespond(withSuccess(geminiResponse("{\"events\":[]}"), MediaType.APPLICATION_JSON));

        assertThat(client.extractFromImage(event, new byte[]{(byte) 0xff, (byte) 0xd8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, null).events())
                .isEmpty();
    }

    @Test
    void rejectsSchemaViolationsAndInvalidDates() throws Exception {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(":generateContent")))
                .andRespond(withSuccess(geminiResponse("{\"events\":[],\"extra\":1}"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.extractFromText(event, "data"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported field");
    }

    @Test
    void validatesConfigurationBeforeHttpCall() {
        GeminiImportClient invalid = new GeminiImportClient(properties(" ", "model"), new ObjectMapper(), RestClient.create());

        assertThatThrownBy(() -> invalid.extractFromText(event, "data"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    void convertsGoogleHttpErrorsToUsefulMessage() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(":generateContent")))
                .andRespond(withBadRequest().body("User location is not supported for the API use"));

        assertThatThrownBy(() -> client.extractFromText(event, "data"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("current network/location");
    }

    private GeminiProperties properties(String key, String model) {
        return new GeminiProperties(key, "https://gemini.test/", model, 5);
    }

    private String geminiResponse(String json) throws Exception {
        return new ObjectMapper().writeValueAsString(java.util.Map.of("candidates", java.util.List.of(
                java.util.Map.of("content", java.util.Map.of("parts", java.util.List.of(java.util.Map.of("text", json)))))));
    }

    private String validEventsJson() {
        return """
                {"events":[{"title":" Cup ","startDate":"2026-01-02","endDate":null,
                "competitionLevel":"NATIONAL","location":"Moscow","externalUrl":null,
                "disciplines":[" Sprint ",""],"priority":"IMPORTANT","sourceReference":12,"rawText":"row"}]}
                """;
    }
}
