package com.flaver.dto.export;

import java.time.LocalDateTime;
import java.util.UUID;

public record CalendarExportEvent(
        UUID id,
        String title,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String timezone,
        String location,
        String distance,
        String priority,
        String status,
        String source,
        String externalUrl,
        String notes
) {
}
