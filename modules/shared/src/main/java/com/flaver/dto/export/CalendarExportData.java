package com.flaver.dto.export;

import java.util.List;
import java.util.UUID;

public record CalendarExportData(
        UUID calendarId,
        String calendarName,
        String sportType,
        Integer year,
        List<CalendarExportEvent> events
) {
}
