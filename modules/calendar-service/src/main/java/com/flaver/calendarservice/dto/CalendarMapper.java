package com.flaver.calendarservice.dto;

import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.Event;

import java.util.ArrayList;

public final class CalendarMapper {
    private CalendarMapper() {
    }

    public static CalendarResponse toCalendarResponse(Calendar calendar) {
        return new CalendarResponse(
                calendar.getId(),
                calendar.getOwnerId(),
                calendar.getName(),
                calendar.getSportType(),
                calendar.getYear(),
                calendar.getCreatedAt(),
                calendar.getUpdatedAt()
        );
    }

    public static EventResponse toEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getCalendarId(),
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getCompetitionLevel() != null ? event.getCompetitionLevel().name() : null,
                event.getCompetitionLevel() != null ? event.getCompetitionLevel().getDisplayNameRu() : null,
                event.getLocation(),
                event.getExternalUrl(),
                event.getDisciplines() != null ? new ArrayList<>(event.getDisciplines()) : new ArrayList<>(),
                event.getPriority() != null ? event.getPriority().name() : null,
                event.getPriority() != null ? event.getPriority().getDisplayNameRu() : null,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
