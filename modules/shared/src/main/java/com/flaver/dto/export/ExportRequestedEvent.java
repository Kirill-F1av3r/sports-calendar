package com.flaver.dto.export;

import java.util.UUID;

public record ExportRequestedEvent(
        UUID jobId,
        UUID userId,
        UUID calendarId,
        String provider
) {
}
