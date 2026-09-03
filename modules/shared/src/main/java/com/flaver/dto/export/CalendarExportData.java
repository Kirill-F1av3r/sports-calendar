package com.flaver.dto.export;

import java.util.List;
import java.util.UUID;

public record CalendarExportData(
        UUID calendarId,
        String calendarName,
        Integer year,
        String goal,
        List<CalendarExportEvent> events
) {
}
