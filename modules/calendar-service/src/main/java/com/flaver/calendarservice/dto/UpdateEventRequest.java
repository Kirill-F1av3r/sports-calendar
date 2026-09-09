package com.flaver.calendarservice.dto;

import java.time.LocalDate;
import java.util.List;

public record UpdateEventRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String competitionLevel,
        String location,
        String externalUrl,
        List<String> disciplines,
        String priority
) {
}
