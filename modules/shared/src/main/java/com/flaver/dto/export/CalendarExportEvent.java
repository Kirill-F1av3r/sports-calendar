package com.flaver.dto.export;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CalendarExportEvent(
        UUID id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String competitionLevel,
        String location,
        List<String> disciplines,
        String priority,
        String externalUrl,
        String sportType
) {
}
