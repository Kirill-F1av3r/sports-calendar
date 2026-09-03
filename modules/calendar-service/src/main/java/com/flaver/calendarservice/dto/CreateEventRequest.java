package com.flaver.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank String title,
        @NotNull LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        @NotBlank String timezone,
        String location,
        String distance,
        String priority,
        String status,
        String source,
        String externalUrl,
        String notes
) {
    public CreateEventRequest(String title, LocalDate startDate, LocalDate endDate, String location, String source) {
        this(title, startDate.atStartOfDay(), endDate.atStartOfDay(), "UTC", location, null, null, null, source, null, null);
    }
}
