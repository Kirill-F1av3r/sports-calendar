package com.flaver.exportworker.dto;

public record CompleteExportRequest(
        String spreadsheetId,
        String spreadsheetUrl
) {
}
