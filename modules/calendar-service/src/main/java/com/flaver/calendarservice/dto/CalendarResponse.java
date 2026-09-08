package com.flaver.calendarservice.dto;

import java.time.Instant;
import java.util.UUID;

public record CalendarResponse(
        UUID id,
        UUID ownerId,
        String name,
        String sportType,
        Integer year,
        Instant createdAt,
        Instant updatedAt
) {
}
