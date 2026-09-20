package com.flaver.dto.importing;

import java.util.UUID;

public record ImportRequestedEvent(
        UUID jobId,
        UUID userId,
        UUID calendarId,
        Integer calendarYear,
        String sportType,
        String objectKey,
        String fileName,
        String contentType
) {
}
