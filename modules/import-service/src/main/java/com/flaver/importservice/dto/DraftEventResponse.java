package com.flaver.importservice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DraftEventResponse(
        UUID id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String competitionLevel,
        String location,
        String externalUrl,
        List<String> disciplines,
        String priority,
        boolean valid,
        String sourceReference,
        String rawText,
        List<DraftEventErrorResponse> errors
) {
}
