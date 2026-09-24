package com.flaver.exportservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateExportRequest(
        @NotNull(message = "calendarId обязателен")
        UUID calendarId,

        @NotBlank(message = "Провайдер экспорта обязателен")
        @Pattern(regexp = "GOOGLE_SHEETS", message = "Провайдер экспорта должен быть GOOGLE_SHEETS")
        String provider
) {
}
