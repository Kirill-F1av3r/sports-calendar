package com.flaver.importworker.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaver.dto.importing.ImportRequestedEvent;
import com.flaver.importworker.config.GeminiProperties;
import com.flaver.importworker.dto.CompleteImportRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiImportClient implements AiImportClient {
    private static final Set<String> ROOT_FIELDS = Set.of("events");
    private static final Set<String> EVENT_FIELDS = Set.of(
            "title",
            "startDate",
            "endDate",
            "competitionLevel",
            "location",
            "externalUrl",
            "disciplines",
            "priority",
            "sourceReference",
            "rawText"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;

    public GeminiImportClient(GeminiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createRestClient(properties));
    }

    GeminiImportClient(GeminiProperties properties, ObjectMapper objectMapper, RestClient restClient) {
        this.restClient = restClient;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.properties = properties;
    }

    private static RestClient createRestClient(GeminiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, properties.requestTimeoutSeconds())))
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Override
    public ImportedEvents extractFromText(ImportRequestedEvent event, String extractedText) {
        String prompt = buildExtractionPrompt(event, extractedText);
        String responseText = generateContent(List.of(textPart(prompt)));
        return parseStrict(responseText);
    }

    @Override
    public ImportedEvents extractFromImage(ImportRequestedEvent event, byte[] imageBytes, String sourceDescription) {
        String prompt = buildExtractionPrompt(
                event,
                """
                        The attached image contains a sports competition calendar.
                        Source description: %s.
                        Extract events from this image only.
                        """.formatted(sourceDescription == null ? "uploaded image" : sourceDescription)
        );
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = detectImageMimeType(imageBytes);
        String responseText = generateContent(List.of(textPart(prompt), inlineDataPart(mimeType, base64)));
        return parseStrict(responseText);
    }

    private String generateContent(List<Map<String, Object>> parts) {
        validateConfiguration();
        GeminiGenerateContentResponse response;
        try {
            response = restClient.post()
                    .uri(generateContentUrl())
                    .body(Map.of(
                            "contents", List.of(Map.of(
                                    "role", "user",
                                    "parts", parts
                            )),
                            "generationConfig", Map.of(
                                    "temperature", 0.1,
                                    "response_mime_type", "application/json",
                                    "response_schema", eventsSchema()
                            )
                    ))
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(buildGeminiHttpErrorMessage(ex), ex);
        } catch (ResourceAccessException ex) {
            Throwable root = rootCause(ex);
            throw new IllegalStateException(
                    "Cannot reach Gemini API from import-worker container: %s. Check Docker network, VPN/proxy settings, or switch IMPORT_AI_PROVIDER back to ollama."
                            .formatted(root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage()),
                    ex
            );
        }

        String text = extractResponseText(response);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Gemini response is empty");
        }
        return text;
    }

    private String buildGeminiHttpErrorMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
        if (body != null && body.contains("User location is not supported for the API use")) {
            return "Gemini API is not available from the current network/location. Use a VPN/proxy available inside Docker, choose another AI provider, or switch IMPORT_AI_PROVIDER back to ollama.";
        }
        String normalizedBody = body == null || body.isBlank()
                ? "empty response body"
                : body.replace("\r", "").replace("\n", " ");
        return "Gemini API request failed: %s. Response: %s".formatted(ex.getStatusCode(), normalizedBody);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void validateConfiguration() {
        if (isBlank(properties.apiKey())) {
            throw new IllegalStateException("GEMINI_API_KEY is required when IMPORT_AI_PROVIDER=gemini");
        }
        if (isBlank(properties.model())) {
            throw new IllegalStateException("GEMINI_MODEL is required when IMPORT_AI_PROVIDER=gemini");
        }
    }

    private String generateContentUrl() {
        String baseUrl = isBlank(properties.baseUrl())
                ? "https://generativelanguage.googleapis.com"
                : properties.baseUrl().replaceAll("/+$", "");
        String model = properties.model().startsWith("models/")
                ? properties.model()
                : "models/" + properties.model();
        return baseUrl + "/v1beta/" + model + ":generateContent?key="
                + URLEncoder.encode(properties.apiKey(), StandardCharsets.UTF_8);
    }

    private String extractResponseText(GeminiGenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (Part part : candidate.content().parts()) {
            if (part.text() != null) {
                result.append(part.text());
            }
        }
        return result.toString();
    }

    private Map<String, Object> textPart(String text) {
        return Map.of("text", text);
    }

    private Map<String, Object> inlineDataPart(String mimeType, String base64) {
        return Map.of("inline_data", Map.of(
                "mime_type", mimeType,
                "data", base64
        ));
    }

    private Map<String, Object> eventsSchema() {
        Map<String, Object> nullableString = Map.of("type", "STRING", "nullable", true);
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "events", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "title", nullableString,
                                                "startDate", nullableString,
                                                "endDate", nullableString,
                                                "competitionLevel", Map.of(
                                                        "type", "STRING",
                                                        "nullable", true,
                                                        "enum", List.of("INTERNATIONAL", "NATIONAL", "REGIONAL", "LOCAL", "TRAINING", "OTHER")
                                                ),
                                                "location", nullableString,
                                                "externalUrl", nullableString,
                                                "disciplines", Map.of(
                                                        "type", "ARRAY",
                                                        "items", Map.of("type", "STRING")
                                                ),
                                                "priority", Map.of(
                                                        "type", "STRING",
                                                        "nullable", true,
                                                        "enum", List.of("REQUIRED", "IMPORTANT", "OPTIONAL")
                                                ),
                                                "sourceReference", nullableString,
                                                "rawText", nullableString
                                        ),
                                        "required", List.of(
                                                "title",
                                                "startDate",
                                                "endDate",
                                                "competitionLevel",
                                                "location",
                                                "externalUrl",
                                                "disciplines",
                                                "priority",
                                                "sourceReference",
                                                "rawText"
                                        )
                                )
                        )
                ),
                "required", List.of("events")
        );
    }

    private ImportedEvents parseStrict(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            validateObjectFields(root, ROOT_FIELDS, "root");

            JsonNode eventsNode = root.get("events");
            if (eventsNode == null || !eventsNode.isArray()) {
                throw new IllegalArgumentException("events must be an array");
            }

            List<CompleteImportRequest.ImportedDraftEventRequest> events = new ArrayList<>();
            for (JsonNode node : eventsNode) {
                events.add(parseStrictEvent(node));
            }
            return new ImportedEvents(events);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Gemini response is not valid events JSON", ex);
        }
    }

    private CompleteImportRequest.ImportedDraftEventRequest parseStrictEvent(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("event must be an object");
        }
        validateObjectFields(node, EVENT_FIELDS, "event");
        for (String field : EVENT_FIELDS) {
            if (!node.has(field)) {
                throw new IllegalArgumentException("event is missing field: " + field);
            }
        }

        return new CompleteImportRequest.ImportedDraftEventRequest(
                textOrNull(node.get("title"), "title"),
                isoDateOrNull(node.get("startDate"), "startDate"),
                isoDateOrNull(node.get("endDate"), "endDate"),
                textOrNull(node.get("competitionLevel"), "competitionLevel"),
                textOrNull(node.get("location"), "location"),
                textOrNull(node.get("externalUrl"), "externalUrl"),
                stringArray(node.get("disciplines")),
                textOrNull(node.get("priority"), "priority"),
                sourceReferenceOrNull(node.get("sourceReference")),
                textOrNull(node.get("rawText"), "rawText")
        );
    }

    private void validateObjectFields(JsonNode node, Set<String> allowedFields, String objectName) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(objectName + " must be an object");
        }
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowedFields.contains(name)) {
                throw new IllegalArgumentException(objectName + " contains unsupported field: " + name);
            }
        }
    }

    private String textOrNull(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new IllegalArgumentException(fieldName + " must be string or null");
        }
        String text = node.asText().trim();
        return text.isEmpty() || text.equalsIgnoreCase("null") || text.equals("-") ? null : text;
    }

    private String sourceReferenceOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return textOrNull(node, "sourceReference");
        }
        if (node.isIntegralNumber()) {
            return node.asText();
        }
        throw new IllegalArgumentException("sourceReference must be string, integer or null");
    }

    private LocalDate isoDateOrNull(JsonNode node, String fieldName) {
        String text = textOrNull(node, fieldName);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " must use yyyy-MM-dd format");
        }
    }

    private List<String> stringArray(JsonNode node) {
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("disciplines must be an array");
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("disciplines must be an array");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("disciplines must contain only strings");
            }
            String text = item.asText().trim();
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    private String buildExtractionPrompt(ImportRequestedEvent event, String data) {
        return """
                Extract sports competitions from the source data.
                Return only events that are present in the source data.

                Field rules:
                - title: competition name only, without date and location when possible.
                - startDate/endDate: ISO yyyy-MM-dd. If there is one date, both dates are equal.
                - competitionLevel: INTERNATIONAL, NATIONAL, REGIONAL, LOCAL, TRAINING, OTHER or null.
                - externalUrl: URL from source, or null.
                - disciplines: empty array when absent.
                - priority: REQUIRED, IMPORTANT, OPTIONAL or null. If absent, use null.
                - sourceReference: source row/page/line when available.
                - rawText: original row or fragment used for the event.
                - Calendar year: %s.
                - Sport type: %s.

                Source data:
                %s
                """.formatted(
                event.calendarYear() != null ? event.calendarYear() : "unknown",
                event.sportType() != null ? event.sportType() : "unknown",
                data
        );
    }

    private String detectImageMimeType(byte[] bytes) {
        if (bytes != null && bytes.length >= 12) {
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return "image/png";
            }
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
                return "image/jpeg";
            }
            if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
                return "image/webp";
            }
        }
        return "image/png";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiGenerateContentResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidate(Content content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Part(String text) {
    }
}
