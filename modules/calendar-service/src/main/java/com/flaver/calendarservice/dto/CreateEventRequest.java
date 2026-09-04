package com.flaver.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateEventRequest(
        @NotBlank String title,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        String competitionLevel,
        String location,
        String externalUrl,
        List<String> disciplines,
        String priority
) {
    public CreateEventRequest(String title, LocalDate startDate, LocalDate endDate, String location, String externalUrl) {
        this(title, startDate, endDate, null, location, externalUrl, null, null);
    }
}
