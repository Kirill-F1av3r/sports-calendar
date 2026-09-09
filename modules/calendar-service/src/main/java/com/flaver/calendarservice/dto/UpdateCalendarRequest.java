package com.flaver.calendarservice.dto;

public record UpdateCalendarRequest(
        String name,
        String sportType,
        Integer year
) {
}
