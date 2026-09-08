package com.flaver.calendarservice.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID calendarId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String competitionLevel,
        String competitionLevelTitle,
        String location,
        String externalUrl,
        List<String> disciplines,
        String priority,
        String priorityTitle,
        Instant createdAt,
        Instant updatedAt
) {
}
