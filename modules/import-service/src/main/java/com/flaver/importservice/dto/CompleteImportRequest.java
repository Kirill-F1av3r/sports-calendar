package com.flaver.importservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CompleteImportRequest(
        @NotNull(message = "Список событий обязателен")
        List<@Valid ImportedDraftEventRequest> events
) {
    public record ImportedDraftEventRequest(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String competitionLevel,
            String location,
            String externalUrl,
            List<String> disciplines,
            String priority,
            String sourceReference,
            String rawText
    ) {
    }
}
