package com.flaver.exportworker.service;

import com.flaver.dto.export.CalendarExportData;
import com.flaver.dto.export.CalendarExportEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GoogleSheetsClient {
    private static final String SHEETS_API_BASE_URL = "https://sheets.googleapis.com/v4/spreadsheets";
    private static final String SHEET_TITLE = "Соревнования";

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
        values.add(List.of("Дата начала", "Дата окончания", "Название", "Уровень", "Дисциплины", "Место",
                "Приоритет", "Ссылка"));
        for (CalendarExportEvent event : data.events()) {
            values.add(List.of(
                    event.startDate() != null ? event.startDate().toString() : "",
                    event.endDate() != null ? event.endDate().toString() : "",
                    value(event.title()),
                    value(event.competitionLevel()),
                    disciplines(event.disciplines()),
                    value(event.location()),
                    value(event.priority()),
                    value(event.externalUrl())
            ));
        }

        String range = "'" + SHEET_TITLE + "'!A1:H" + values.size();
        restClient.put()
                .uri(SHEETS_API_BASE_URL + "/{spreadsheetId}/values/{range}?valueInputOption=RAW",
                        spreadsheetId, range)
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

    private String disciplines(List<String> disciplines) {
        if (disciplines == null || disciplines.isEmpty()) {
            return "";
        }
        return disciplines.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String value(String value) {
        return value != null ? value : "";
    }

    public record GoogleSpreadsheetResult(String spreadsheetId, String spreadsheetUrl) {
    }
}
