package com.flaver.exportservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteExportRequest(
        @NotBlank String spreadsheetId,
        @NotBlank String spreadsheetUrl
) {
}
