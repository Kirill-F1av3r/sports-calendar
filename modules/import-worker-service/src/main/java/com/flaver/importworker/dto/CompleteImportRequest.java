package com.flaver.importworker.dto;

import java.time.LocalDate;
import java.util.List;

public record CompleteImportRequest(List<ImportedDraftEventRequest> events) {
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
