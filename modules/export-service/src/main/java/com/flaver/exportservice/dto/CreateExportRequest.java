package com.flaver.exportservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateExportRequest(
        @NotNull UUID calendarId,
        @NotBlank String provider
) {
}
