package com.flaver.calendarservice.dto;

import java.util.List;

public record CalendarDetailsResponse(CalendarResponse calendar, List<EventResponse> events) {
}
