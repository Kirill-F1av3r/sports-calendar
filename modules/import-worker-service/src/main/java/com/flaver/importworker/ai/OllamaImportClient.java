package com.flaver.importworker.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaver.dto.importing.ImportRequestedEvent;
import com.flaver.importworker.config.OllamaProperties;
import com.flaver.importworker.dto.CompleteImportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaImportClient implements AiImportClient {
    private static final Logger log = LoggerFactory.getLogger(OllamaImportClient.class);
    private static final int MAX_LOGGED_AI_RESPONSE_LENGTH = 6000;
    private static final int MAX_REPAIR_INPUT_LENGTH = 12000;
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
    private final OllamaProperties properties;

    public OllamaImportClient(OllamaProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createRestClient(properties));
    }

    OllamaImportClient(OllamaProperties properties, ObjectMapper objectMapper, RestClient restClient) {
        this.restClient = restClient;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.properties = properties;
    }

    private static RestClient createRestClient(OllamaProperties properties) {
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
        String rawResponse = chatContent(properties.textModel(), prompt, null);
        return parseStrictOrRepair(event, rawResponse);
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
        String rawResponse = chatContent(properties.visionModel(), prompt, List.of(base64));
        return parseStrictOrRepair(event, rawResponse);
    }

    private String chatContent(String model, String prompt, List<String> images) {
        OllamaChatResponse response = chat(new OllamaChatRequest(
                model,
                false,
                properties.keepAlive(),
                "json",
                new Options(properties.numCtx(), properties.numPredict(), 0.1),
                List.of(new Message("user", prompt, images))
        ));
        if (response == null || response.message() == null) {
            throw new IllegalArgumentException("AI response is empty");
        }
        return response.message().content();
    }

    private OllamaChatResponse chat(OllamaChatRequest request) {
        return restClient.post()
                .uri(properties.baseUrl() + "/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);
    }

    private ImportedEvents parseStrictOrRepair(ImportRequestedEvent event, String rawResponse) {
        try {
            return parseStrict(rawResponse);
        } catch (RuntimeException firstError) {
            if (rawResponse != null && rawResponse.length() > MAX_REPAIR_INPUT_LENGTH) {
                throw new IllegalArgumentException("AI returned invalid large events JSON", firstError);
            }
            log.warn("AI response does not match strict import schema. Trying repair request. Reason: {}", firstError.getMessage());
            String repairPrompt = buildRepairPrompt(event, rawResponse);
            String repairedResponse = chatContent(properties.textModel(), repairPrompt, null);
            try {
                return parseStrict(repairedResponse);
            } catch (RuntimeException repairError) {
                log.error("AI returned invalid events JSON. Raw response: {}", abbreviate(rawResponse));
                log.error("AI repair also returned invalid events JSON. Repaired response: {}", abbreviate(repairedResponse));
                throw new IllegalArgumentException("AI returned invalid events JSON", repairError);
            }
        }
    }

    private ImportedEvents parseStrict(String content) {
        try {
            String json = extractJsonObject(content);
            JsonNode root = objectMapper.readTree(json);
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
            throw new IllegalArgumentException("AI response is not valid JSON", ex);
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

    private String extractJsonObject(String content) {
        if (content == null) {
            throw new IllegalArgumentException("AI response is empty");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("AI response does not contain JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private String abbreviate(String content) {
        if (content == null) {
            return "<null>";
        }
        if (content.length() <= MAX_LOGGED_AI_RESPONSE_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_LOGGED_AI_RESPONSE_LENGTH) + "... <truncated>";
    }

    private String buildExtractionPrompt(ImportRequestedEvent event, String data) {
        return """
                You extract sports competitions from a calendar file.
                Return ONLY valid JSON. No markdown. No comments.

                You MUST use exactly this JSON schema:
                {
                  "events": [
                    {
                      "title": "string or null",
                      "startDate": "yyyy-MM-dd or null",
                      "endDate": "yyyy-MM-dd or null",
                      "competitionLevel": "INTERNATIONAL|NATIONAL|REGIONAL|LOCAL|TRAINING|OTHER|null",
                      "location": "string or null",
                      "externalUrl": "string or null",
                      "disciplines": ["string"],
                      "priority": "REQUIRED|IMPORTANT|OPTIONAL|null",
                      "sourceReference": "string or null",
                      "rawText": "string or null"
                    }
                  ]
                }

                Strict rules:
                - Do not use any other field names.
                - Extract every competition event from the source data.
                - Do not summarize several source rows into one event.
                - Do not stop after examples. Continue until all events from the source data are extracted.
                - Use "title", not "name".
                - Use "startDate" and "endDate", not "date".
                - Use "disciplines", not "type".
                - Dates must be ISO format yyyy-MM-dd.
                - If there is one date, startDate and endDate must be equal.
                - If a value is unknown, use JSON null.
                - If priority is absent in the source, use JSON null. Do not invent OPTIONAL.
                - Do not invent URL, location, disciplines, level or priority.
                - disciplines must be an empty array when disciplines are absent.
                - sourceReference should contain the source row number or page when it is available.
                - sourceReference must be a JSON string, for example "Row 61", not a number.
                - rawText should contain the original row or text fragment used for this event.
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

    private String buildRepairPrompt(ImportRequestedEvent event, String previousResponse) {
        return """
                Your previous response is valid JSON, but it does NOT match the required schema.
                Convert it to the required schema.
                Return ONLY valid JSON. No markdown. No comments.

                Required schema:
                {
                  "events": [
                    {
                      "title": "string or null",
                      "startDate": "yyyy-MM-dd or null",
                      "endDate": "yyyy-MM-dd or null",
                      "competitionLevel": "INTERNATIONAL|NATIONAL|REGIONAL|LOCAL|TRAINING|OTHER|null",
                      "location": "string or null",
                      "externalUrl": "string or null",
                      "disciplines": ["string"],
                      "priority": "REQUIRED|IMPORTANT|OPTIONAL|null",
                      "sourceReference": "string or null",
                      "rawText": "string or null"
                    }
                  ]
                }

                Mapping hints:
                - name -> title
                - date -> startDate and endDate
                - type/discipline/distance -> disciplines
                - place/venue -> location
                - link/url -> externalUrl
                - Dates like 30.08.2026 must become 2026-08-30.
                - If there is one date, startDate and endDate must be equal.
                - If priority is absent, use JSON null.
                - sourceReference must be a JSON string, for example "Row 61", not a number.
                - Ignore unsupported fields such as coordinates, dayOfWeek and details.
                - Do not add events that were not present in the previous response.
                - Calendar year: %s.
                - Sport type: %s.

                Previous response:
                %s
                """.formatted(
                event.calendarYear() != null ? event.calendarYear() : "unknown",
                event.sportType() != null ? event.sportType() : "unknown",
                previousResponse
        );
    }

    private record OllamaChatRequest(String model,
                                     boolean stream,
                                     String keep_alive,
                                     String format,
                                     Options options,
                                     List<Message> messages) {
    }

    private record Options(int num_ctx, int num_predict, double temperature) {
    }

    private record Message(String role, String content, List<String> images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaChatResponse(MessageResponse message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MessageResponse(String content) {
    }
}
