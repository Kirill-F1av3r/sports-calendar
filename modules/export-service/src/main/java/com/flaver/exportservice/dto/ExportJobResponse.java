package com.flaver.exportservice.dto;

public record ExportJobResponse(
        String jobId,
        String provider,
        String status,
        String spreadsheetId,
        String spreadsheetUrl,
        String errorMessage
) {
}
