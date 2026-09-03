package com.flaver.exportworker.service;

import com.flaver.dto.export.CalendarExportData;
import com.flaver.dto.export.CalendarExportEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GoogleSheetsClient {
    private static final String SHEETS_API_BASE_URL = "https://sheets.googleapis.com/v4/spreadsheets";
    private static final String SHEET_TITLE = "Competitions";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestClient restClient;

    public GoogleSheetsClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public GoogleSpreadsheetResult export(CalendarExportData data, String accessToken) {
        GoogleSpreadsheetResult spreadsheet = createSpreadsheet(data, accessToken);
        writeRows(spreadsheet.spreadsheetId(), data, accessToken);
        return spreadsheet;
    }

    private GoogleSpreadsheetResult createSpreadsheet(CalendarExportData data, String accessToken) {
        Map<String, Object> body = Map.of(
                "properties", Map.of("title", spreadsheetTitle(data)),
                "sheets", List.of(Map.of("properties", Map.of("title", SHEET_TITLE)))
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(SHEETS_API_BASE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("spreadsheetId") == null || response.get("spreadsheetUrl") == null) {
            throw new IllegalStateException("Google Sheets API returned invalid create spreadsheet response");
        }

        return new GoogleSpreadsheetResult(
                response.get("spreadsheetId").toString(),
                response.get("spreadsheetUrl").toString()
        );
    }

    private void writeRows(String spreadsheetId, CalendarExportData data, String accessToken) {
        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("Start", "End", "Timezone", "Title", "Distance", "Location", "Priority", "Status",
                "Source", "URL", "Notes"));
        for (CalendarExportEvent event : data.events()) {
            values.add(List.of(
                    event.startDateTime() != null ? event.startDateTime().format(DATE_TIME_FORMATTER) : "",
                    event.endDateTime() != null ? event.endDateTime().format(DATE_TIME_FORMATTER) : "",
                    value(event.timezone()),
                    value(event.title()),
                    value(event.distance()),
                    value(event.location()),
                    value(event.priority()),
                    value(event.status()),
                    value(event.source()),
                    value(event.externalUrl()),
                    value(event.notes())
            ));
        }

        String range = UriUtils.encodePathSegment(SHEET_TITLE + "!A1:K" + values.size(), StandardCharsets.UTF_8);
        restClient.put()
                .uri(SHEETS_API_BASE_URL + "/" + spreadsheetId + "/values/" + range + "?valueInputOption=RAW")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(Map.of("values", values))
                .retrieve()
                .toBodilessEntity();
    }

    private String spreadsheetTitle(CalendarExportData data) {
        String name = data.calendarName() != null && !data.calendarName().isBlank()
                ? data.calendarName().trim()
                : "Sports Calendar Export";
        return data.year() != null ? name + " " + data.year() : name;
    }

    private String value(String value) {
        return value != null ? value : "";
    }

    public record GoogleSpreadsheetResult(String spreadsheetId, String spreadsheetUrl) {
    }
}
