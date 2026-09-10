package com.flaver.exportservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteExportRequest(
        @NotBlank(message = "spreadsheetId не должен быть пустым")
        @Size(max = 255, message = "spreadsheetId должен быть не длиннее 255 символов")
        String spreadsheetId,

        @NotBlank(message = "spreadsheetUrl не должен быть пустым")
        @Size(max = 500, message = "spreadsheetUrl должен быть не длиннее 500 символов")
        String spreadsheetUrl
) {
}
